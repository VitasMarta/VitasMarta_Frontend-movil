package co.vitasmarta.mobile.feature.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
class RegistrationConsent(val finalidad: String, val otorgado: Boolean)

@Serializable
class RegistrationRequest(
    val correo: String,
    val password: String,
    @SerialName("tipo_usuario") val userType: String,
    val consentimientos: List<RegistrationConsent>
) {
    override fun toString(): String = "RegistrationRequest(redacted)"
}

@Serializable
class RegistrationResponse(val mensaje: String)

interface RegistrationApi {
    @POST("api/v1/identidad/cuentas")
    suspend fun register(@Body request: RegistrationRequest): Response<RegistrationResponse>
}
