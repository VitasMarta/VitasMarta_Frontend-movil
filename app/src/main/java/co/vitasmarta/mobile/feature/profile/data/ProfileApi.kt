package co.vitasmarta.mobile.feature.profile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

@Serializable
class MemberProfileDto(
    @SerialName("usuario_ref") val userId: String,
    @SerialName("nombre_visible") val visibleName: String? = null,
    @SerialName("fecha_actualizacion") val updatedAt: String? = null
)

@Serializable
class NameUpdate(@SerialName("nombre_visible") val name: String) {
    override fun toString() = "NameUpdate(redacted)"
}

@Serializable
class ProfileMessage(val mensaje: String)

@Serializable
class PreferencesDto(
    @SerialName("idioma_preferido") val language: String,
    @SerialName("idioma_autorizado") val mailLanguage: String,
    @SerialName("canal_autorizado") val channel: String
)

@Serializable
class LanguageUpdate(@SerialName("idioma_preferido") val language: String)

@Serializable
class ConsentDto(
    val otorgado: Boolean,
    @SerialName("version_politica") val version: String? = null,
    @SerialName("fecha_hora") val at: String? = null
)

@Serializable
class ConsentsDto(val vigente: Map<String, ConsentDto?>)

@Serializable
class ConsentUpdate(val otorgado: Boolean)

@Serializable
class InterestsUpdate(val intereses: List<String>)

@Serializable
class ProfileCategoryDto(val slug: String, val nombre: Map<String, String>)

@Serializable
class RightsStepDto(
    val paso: String,
    @SerialName("marca_temporal") val at: String? = null,
    val resultado: String? = null
)

@Serializable
class RightsCaseDto(
    @SerialName("solicitud_ref") val reference: String,
    val tipo: String,
    val estado: String,
    @SerialName("fecha_recepcion") val receivedAt: String? = null,
    val pasos: List<RightsStepDto> = emptyList()
)

@Serializable
class RightsSubmission(val tipo: String, val detalle: Map<String, String>) {
    override fun toString() = "RightsSubmission(redacted)"
}

@Serializable
class RightsReceiptDto(@SerialName("solicitud_ref") val reference: String, val mensaje: String)

interface ProfileCategoriesApi {
    @GET("api/v1/catalogo/categorias")
    suspend fun categories(): Response<List<ProfileCategoryDto>>
}

interface ProfileApi {
    @GET("api/v1/identidad/usuarios/actual/perfil")
    suspend fun profile(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<MemberProfileDto>

    @PUT("api/v1/identidad/usuarios/actual/perfil")
    suspend fun updateName(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String,
        @Body request: NameUpdate
    ): Response<ProfileMessage>

    @GET("api/v1/identidad/usuarios/actual/preferencias")
    suspend fun preferences(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<PreferencesDto>

    @PUT("api/v1/identidad/usuarios/actual/preferencias")
    suspend fun updateLanguage(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String,
        @Body request: LanguageUpdate
    ): Response<PreferencesDto>

    @GET("api/v1/identidad/usuarios/actual/consentimientos")
    suspend fun consents(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<ConsentsDto>

    @PUT("api/v1/identidad/usuarios/actual/consentimientos/{purpose}")
    suspend fun setConsent(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String,
        @Path("purpose") purpose: String,
        @Body request: ConsentUpdate
    ): Response<ProfileMessage>

    @PUT("api/v1/identidad/usuarios/actual/intereses")
    suspend fun replaceInterests(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String,
        @Body request: InterestsUpdate
    ): Response<ProfileMessage>

    @GET("api/v1/identidad/usuarios/actual/solicitudes-arco")
    suspend fun rightsCases(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<List<RightsCaseDto>>

    @GET("api/v1/identidad/usuarios/actual/solicitudes-arco/{reference}")
    suspend fun rightsCase(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String,
        @Path("reference") reference: String
    ): Response<RightsCaseDto>

    @POST("api/v1/identidad/usuarios/actual/solicitudes-arco")
    suspend fun submitRights(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String,
        @Body request: RightsSubmission
    ): Response<RightsReceiptDto>

    @Streaming
    @Headers("Accept: application/pdf")
    @GET("api/v1/identidad/usuarios/actual/exportacion-datos")
    suspend fun exportData(
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<ResponseBody>
}
