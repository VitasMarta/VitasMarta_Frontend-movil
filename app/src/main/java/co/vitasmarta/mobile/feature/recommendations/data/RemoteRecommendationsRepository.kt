package co.vitasmarta.mobile.feature.recommendations.data

import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.AuthenticatedApiExecutor
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationFeed
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationsRepository
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendedAttraction
import java.math.BigDecimal
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class RemoteRecommendationsRepository(
    private val api: RecommendationsApi?,
    private val authenticated: AuthenticatedApiExecutor
) : RecommendationsRepository {
    override suspend fun recommendations(): AppResult<RecommendationFeed> {
        val remote = api ?: return AppResult.Failure(AppError(ErrorKind.Configuration))
        var partial = false
        return when (
            val result = authenticated.execute { bearer, id ->
                remote.recommendations(bearer, id).also { partial = it.code() == 206 }
            }
        ) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val data = result.value
                if (data.recomendaciones.any { it.id.isBlank() || it.nombre.isBlank() } ||
                    data.recomendaciones.map {
                        it.id
                    }.distinct().size != data.recomendaciones.size
                ) {
                    AppResult.Failure(AppError(ErrorKind.InvalidResponse))
                } else {
                    val items = try {
                        data.recomendaciones.map { item ->
                            RecommendedAttraction(
                                item.id, item.nombre, item.explicacion, item.categorias,
                                item.saturation, item.occupancy, item.warning,
                                item.alternatives.filter(String::isNotBlank),
                                money(item.minimumPrice)
                            )
                        }
                    } catch (_: IllegalArgumentException) {
                        return AppResult.Failure(AppError(ErrorKind.InvalidResponse))
                    }
                    AppResult.Success(
                        RecommendationFeed(
                            data.modo,
                            partial || data.degraded || data.modo == "DEGRADADO_FALLBACK",
                            data.personalizada,
                            data.meta.transparency,
                            data.meta.generatedByAi,
                            data.environmentalStateVerified,
                            data.advertencias,
                            items
                        )
                    )
                }
            }
        }
    }

    private fun money(value: JsonPrimitive?): BigDecimal? {
        if (value == null || value is JsonNull) return null
        val raw = value.content
        // Bound input and exponent before parsing; never expand an untrusted exponent.
        require(raw.length <= 64 && MONEY_NUMBER.matches(raw))
        val exponentText = raw.substringAfter('e', raw.substringAfter('E', "0"))
        require(exponentText.removePrefix("+").removePrefix("-").length <= 3)
        val exponent = requireNotNull(exponentText.toIntOrNull())
        require(exponent in -32..32)
        val parsed = requireNotNull(raw.toBigDecimalOrNull())
        require(parsed.signum() >= 0 && parsed.precision() <= 24)
        require(parsed.scale() in -16..8)
        require((parsed.precision() - parsed.scale()).coerceAtLeast(0) <= 16)
        return if (parsed.scale() < 0) parsed.setScale(0) else parsed
    }

    private companion object {
        val MONEY_NUMBER = Regex("""-?[0-9]+(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?""")
    }
}
