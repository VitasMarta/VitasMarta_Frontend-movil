package co.vitasmarta.mobile.feature.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
class LoginRequest(val correo: String, val password: String) {
    override fun toString(): String = "LoginRequest(redacted)"
}

@Serializable
class RefreshRequest(@SerialName("refresh_token") val refreshToken: String) {
    override fun toString(): String = "RefreshRequest(redacted)"
}

@Serializable
class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("mfa_requerido") val mfaRequired: Boolean = false
) {
    override fun toString(): String = "TokenResponse(redacted)"
}

@Serializable
class RecoveryRequest(val correo: String) {
    override fun toString(): String = "RecoveryRequest(redacted)"
}

interface IdentityApi {
    @POST("api/v1/identidad/password/recuperacion")
    suspend fun recover(@Body request: RecoveryRequest): Response<Unit>

    @POST("api/v1/identidad/sesiones")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/v1/identidad/sesiones/refresco")
    suspend fun refresh(@Body request: RefreshRequest): Response<TokenResponse>

    @DELETE("api/v1/identidad/sesiones")
    suspend fun logout(@Header("Authorization") bearer: String): Response<Unit>
}
