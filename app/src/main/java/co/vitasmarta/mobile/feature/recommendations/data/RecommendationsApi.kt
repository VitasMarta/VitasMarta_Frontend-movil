package co.vitasmarta.mobile.feature.recommendations.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

@Serializable
class RecommendationMetaDto(
    @SerialName("aviso_transparencia") val transparency: String,
    @SerialName("generado_por_ia") val generatedByAi: Boolean
)

@Serializable
class RecommendedAttractionDto(
    @SerialName("atractivo_id") val id: String,
    val nombre: String,
    val explicacion: String,
    val categorias: List<String> = emptyList(),
    @SerialName("nivel_saturacion") val saturation: String? = null,
    @SerialName("ocupacion_estimada") val occupancy: Double? = null,
    @SerialName("advertencia_ambiental") val warning: String? = null,
    @SerialName("alternativas_sugeridas") val alternatives: List<String> = emptyList(),
    @SerialName("precio_minimo") val minimumPrice: JsonPrimitive? = null
)

@Serializable
class RecommendationFeedDto(
    val modo: String,
    @SerialName("degradada_fallback") val degraded: Boolean,
    val personalizada: Boolean,
    @SerialName("metadatos_ia") val meta: RecommendationMetaDto,
    @SerialName("estado_ambiental_verificado") val environmentalStateVerified: Boolean,
    val advertencias: List<String> = emptyList(),
    val recomendaciones: List<RecommendedAttractionDto>
)
interface RecommendationsApi {
    @GET("api/v1/recomendaciones")
    suspend fun recommendations(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<RecommendationFeedDto>
}
