package co.vitasmarta.mobile.feature.booking.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@Serializable
data class PolicyDto(
    @SerialName("politica_ref") val reference: String,
    val version: String,
    val contenido: String,
    @SerialName("horas_limite_cancelacion") val cancellationHours: Int? = null,
    @SerialName("penalidad_porcentaje") val penaltyPercent: Int? = null
)

@Serializable
data class OfferDto(
    @SerialName("servicio_ref") val serviceRef: String,
    @SerialName("franja_ref") val slotRef: String,
    val inicio: String,
    val fin: String,
    val importe: String,
    val moneda: String,
    @SerialName("unidad_tarifa") val unit: String,
    @SerialName("capacidad_total") val capacity: Int,
    val disponibles: Int? = null,
    val politica: PolicyDto,
    val reservable: Boolean,
    @SerialName("requiere_aprobacion") val needsApproval: Boolean,
    @SerialName("requiere_pago_externo") val needsExternalPayment: Boolean
)

@Serializable
data class OffersDto(
    @SerialName("servicio_ref") val serviceRef: String,
    val ofertas: List<OfferDto>
)

@Serializable
data class FareDto(@SerialName("importe_total") val total: String, val moneda: String)

@Serializable
data class BookingDto(
    @SerialName("reserva_ref") val reference: String,
    @SerialName("codigo_confirmacion") val confirmationCode: String,
    @SerialName("servicio_ref") val serviceRef: String,
    @SerialName("franja_ref") val slotRef: String,
    @SerialName("cantidad_cupos") val quantity: Int,
    val estado: String,
    @SerialName("fecha_inicio") val startsAt: String,
    @SerialName("fecha_fin") val endsAt: String,
    @SerialName("vence_en") val deadline: String? = null,
    val tarifa: FareDto,
    val politica: PolicyDto,
    @SerialName("instrucciones_pago_externo") val paymentInstructions: String? = null,
    @SerialName("siguiente_accion") val nextAction: String,
    @SerialName("penalidad_aplicada_porcentaje") val penaltyPercent: Int,
    @SerialName("original_ref") val originalRef: String? = null,
    @SerialName("reemplazo_ref") val replacementRef: String? = null,
    @SerialName("ajuste_externo") val externalAdjustment: String? = null
)

@Serializable
data class BookingPageDto(
    val content: List<BookingDto>,
    @SerialName("total_elements") val total: Int,
    val page: Int,
    val size: Int
)

@Serializable
class KeyRequest(val operacion: String, @SerialName("reserva_ref") val reference: String?) {
    override fun toString(): String = "KeyRequest(redacted)"
}

@Serializable
class KeyDto(@SerialName("clave_idempotencia") val key: String, val operacion: String) {
    override fun toString(): String = "KeyDto(redacted)"
}

@Serializable
data class PolicyAcceptanceDto(
    val aceptada: Boolean,
    @SerialName("politica_ref") val reference: String,
    val version: String
)

@Serializable
data class CreateBookingDto(
    @SerialName("servicio_ref") val serviceRef: String,
    @SerialName("franja_ref") val slotRef: String,
    @SerialName("cantidad_cupos") val quantity: Int,
    @SerialName("aceptacion_politica") val policy: PolicyAcceptanceDto
)

@Serializable
class CancelBookingDto(val motivo: String) {
    override fun toString(): String = "CancelBookingDto(redacted)"
}

interface BookingApi {
    @GET("api/v1/reservas/ofertas/{service}")
    suspend fun offers(
        @Path("service") service: String,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<OffersDto>

    @GET("api/v1/reservas")
    suspend fun list(
        @Query("clasificacion") group: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<BookingPageDto>

    @GET("api/v1/reservas/{reference}")
    suspend fun detail(
        @Path("reference") reference: String,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<BookingDto>

    @POST("api/v1/reservas/claves-idempotencia")
    suspend fun issueKey(
        @Body request: KeyRequest,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<KeyDto>

    @POST("api/v1/reservas")
    suspend fun create(
        @Body request: CreateBookingDto,
        @Header("Idempotency-Key") key: String,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<BookingDto>

    @POST("api/v1/reservas/{reference}/cancelar")
    suspend fun cancel(
        @Path("reference") reference: String,
        @Body request: CancelBookingDto,
        @Header("Idempotency-Key") key: String,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<BookingDto>

    @Streaming
    @GET("api/v1/reservas/{reference}/comprobante")
    suspend fun receipt(
        @Path("reference") reference: String,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<ResponseBody>

    @POST("api/v1/reservas/{reference}/modificar")
    suspend fun modify(
        @Path("reference") reference: String,
        @Body request: CreateBookingDto,
        @Header("Idempotency-Key") key: String,
        @Header("Authorization") bearer: String,
        @Header("X-Correlation-Id") correlation: String
    ): Response<BookingDto>
}
