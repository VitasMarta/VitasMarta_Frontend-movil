package co.vitasmarta.mobile.feature.catalog.data

import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.catalog.domain.CatalogAttribute
import co.vitasmarta.mobile.feature.catalog.domain.CatalogContentRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogDetail
import co.vitasmarta.mobile.feature.catalog.domain.CatalogItem
import co.vitasmarta.mobile.feature.catalog.domain.CatalogLocation
import co.vitasmarta.mobile.feature.catalog.domain.CatalogMedia
import co.vitasmarta.mobile.feature.catalog.domain.CatalogMoney
import co.vitasmarta.mobile.feature.catalog.domain.CatalogPage
import co.vitasmarta.mobile.feature.catalog.domain.CatalogSearchCriteria
import co.vitasmarta.mobile.feature.catalog.domain.CatalogSlot
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class RemoteCatalogContentRepository(
    private val api: CatalogApi?,
    private val executor: ApiExecutor
) : CatalogContentRepository {
    override suspend fun search(
        criteria: CatalogSearchCriteria,
        cursor: String?
    ): AppResult<CatalogPage> {
        val remote = api ?: return unavailable()
        if (criteria.minimumPrice?.let { !CatalogMoney.isBounded(it) } == true ||
            criteria.maximumPrice?.let { !CatalogMoney.isBounded(it) } == true
        ) {
            return AppResult.Failure(AppError(ErrorKind.Validation))
        }
        return when (
            val result = executor.execute {
                remote.search(
                    criteria.text.trim().takeIf { it.isNotEmpty() },
                    criteria.categories,
                    criteria.minimumPrice?.toPlainString(),
                    criteria.maximumPrice?.toPlainString(),
                    criteria.order.wire,
                    criteria.descending,
                    20,
                    cursor
                )
            }
        ) {
            is AppResult.Failure -> result
            is AppResult.Success -> checked {
                val dto = result.value
                require(dto.total >= 0 && dto.items.map { it.id }.distinct().size == dto.items.size)
                require(dto.nextCursor == null || dto.nextCursor.isNotBlank())
                CatalogPage(
                    dto.items.map { item ->
                        require(item.id.isNotBlank() && item.nombre.values.any { it.isNotBlank() })
                        CatalogItem(
                            item.id,
                            item.nombre,
                            item.categorias,
                            money(item.minimumPrice),
                            money(item.maximumPrice),
                            safeImage(item.imageUrl)
                        )
                    },
                    dto.total,
                    dto.nextCursor,
                    dto.fromCache,
                    dto.lastUpdated
                )
            }
        }
    }

    override suspend fun detail(id: String, language: String): AppResult<CatalogDetail> {
        val remote = api ?: return unavailable()
        return when (val result = executor.execute { remote.detail(id, language) }) {
            is AppResult.Failure -> result
            is AppResult.Success -> checked {
                val dto = result.value
                require(dto.id == id && id.isNotBlank())
                val location = dto.ubicacion?.let {
                    require(it.lat.isFinite() && it.lat in -90.0..90.0)
                    require(it.lng.isFinite() && it.lng in -180.0..180.0)
                    CatalogLocation(it.lat, it.lng)
                }
                CatalogDetail(
                    dto.id, dto.nombre?.takeIf { it.isNotBlank() },
                    dto.descripcion?.takeIf { it.isNotBlank() },
                    dto.categorias.map { it.nombre }, location,
                    dto.atributos.mapNotNull { (name, value) ->
                        attributeValue(value)?.let { CatalogAttribute(name, it) }
                    },
                    dto.multimedia.distinctBy { it.orden to it.url }.sortedBy { it.orden }
                        .mapNotNull { media ->
                            safeImage(media.url)?.let { CatalogMedia(media.tipo, it, media.orden) }
                        },
                    money(dto.priceRange?.minimo), money(dto.priceRange?.maximo),
                    dto.priceRange?.moneda, dto.franjas.map(::slot), dto.fromCache,
                    dto.frescura?.advertencia == true || !dto.frescura?.avisos.isNullOrEmpty(),
                    dto.frescura?.lastUpdated
                )
            }
        }
    }

    override suspend fun slots(id: String): AppResult<List<CatalogSlot>> {
        val remote = api ?: return unavailable()
        return when (val result = executor.execute { remote.slots(id) }) {
            is AppResult.Failure -> result
            is AppResult.Success -> checked {
                require(result.value.id == id)
                result.value.franjas.map(::slot)
            }
        }
    }

    private fun slot(dto: SlotDto): CatalogSlot {
        require(dto.cupo >= 0 && (dto.disponibles == null || dto.disponibles >= 0))
        require(dto.estado.isNotBlank())
        LocalDate.parse(dto.fecha)
        LocalTime.parse(dto.start)
        LocalTime.parse(dto.end)
        return CatalogSlot(
            dto.id, dto.fecha, dto.start, dto.end, dto.cupo, dto.estado,
            dto.disponibles, dto.validUntil, dto.retirada
        )
    }

    private fun money(value: JsonPrimitive?): BigDecimal? {
        if (value == null || value == JsonNull) return null
        return requireNotNull(CatalogMoney.fromRemote(value.content))
    }

    private fun safeImage(value: String?): String? = value?.toHttpUrlOrNull()?.takeIf {
        it.isHttps && it.username.isEmpty() && it.password.isEmpty()
    }?.toString()

    private fun attributeValue(value: JsonElement): String? = when (value) {
        JsonNull -> null
        is JsonPrimitive -> value.content.takeIf { it.isNotBlank() }
        is JsonArray -> value.mapNotNull(::attributeValue).joinToString(", ")
            .takeIf { it.isNotBlank() }
        is JsonObject -> value.mapNotNull { (key, nested) ->
            attributeValue(nested)?.let { "$key: $it" }
        }.joinToString("; ").takeIf { it.isNotBlank() }
    }

    private inline fun <T> checked(map: () -> T): AppResult<T> = try {
        AppResult.Success(map())
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError(ErrorKind.InvalidResponse))
    } catch (_: java.time.format.DateTimeParseException) {
        AppResult.Failure(AppError(ErrorKind.InvalidResponse))
    }

    private fun unavailable() = AppResult.Failure(AppError(ErrorKind.Configuration))
}
