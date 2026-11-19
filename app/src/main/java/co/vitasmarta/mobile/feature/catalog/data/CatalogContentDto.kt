package co.vitasmarta.mobile.feature.catalog.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
class CatalogItemDto(
    val id: String,
    val nombre: Map<String, String>,
    val categorias: List<String>,
    @SerialName("precio_minimo") val minimumPrice: JsonPrimitive? = null,
    @SerialName("precio_maximo") val maximumPrice: JsonPrimitive? = null,
    @SerialName("imagen_url") val imageUrl: String? = null
)

@Serializable
class CatalogPageDto(
    val items: List<CatalogItemDto> = emptyList(),
    val total: Long = 0,
    @SerialName("cursor_siguiente") val nextCursor: String? = null,
    @SerialName("desde_cache") val fromCache: Boolean = false,
    @SerialName("frescura_ultima_actualizacion") val lastUpdated: String? = null
)

@Serializable
class DetailCategoryDto(val slug: String, val nombre: String)

@Serializable
class LocationDto(val lat: Double, val lng: Double)

@Serializable
class MediaDto(val tipo: String, @SerialName("url_cdn") val url: String, val orden: Int)

@Serializable
class PriceRangeDto(
    val minimo: JsonPrimitive? = null,
    val maximo: JsonPrimitive? = null,
    val moneda: String = "COP"
)

@Serializable
class FreshnessDto(
    @SerialName("ultima_actualizacion") val lastUpdated: String? = null,
    val advertencia: Boolean = false,
    val avisos: List<String> = emptyList()
)

@Serializable
class SlotDto(
    val id: String? = null,
    val fecha: String,
    @SerialName("hora_inicio") val start: String,
    @SerialName("hora_fin") val end: String,
    val cupo: Int,
    val estado: String,
    val disponibles: Int? = null,
    @SerialName("vigente_hasta") val validUntil: String? = null,
    val retirada: Boolean = false
)

@Serializable
class SlotsDto(val id: String, val franjas: List<SlotDto> = emptyList())

@Serializable
class CatalogDetailDto(
    val id: String,
    val nombre: String? = null,
    val descripcion: String? = null,
    val categorias: List<DetailCategoryDto> = emptyList(),
    val ubicacion: LocationDto? = null,
    val atributos: Map<String, JsonElement> = emptyMap(),
    val multimedia: List<MediaDto> = emptyList(),
    @SerialName("rango_precio") val priceRange: PriceRangeDto? = null,
    val franjas: List<SlotDto> = emptyList(),
    val frescura: FreshnessDto? = null,
    @SerialName("desde_cache") val fromCache: Boolean = false
)
