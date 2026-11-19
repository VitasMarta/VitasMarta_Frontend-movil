package co.vitasmarta.mobile.feature.booking.domain

import co.vitasmarta.mobile.core.common.AppResult
import java.math.BigDecimal
import java.time.Instant

enum class BookingGroup { ACTIVAS, PASADAS, CANCELADAS }
enum class BookingStatus {
    SOLICITADA,
    PENDIENTE_PAGO_EXTERNO,
    PENDIENTE_CONFIRMACION_PRESTADOR,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    EXPIRADA,
    RECHAZADA
}

data class BookingPolicy(
    val reference: String,
    val version: String,
    val content: String,
    val cancellationHours: Int? = null,
    val penaltyPercent: Int? = null
)

data class BookingOffer(
    val serviceRef: String,
    val slotRef: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val amount: BigDecimal,
    val currency: String,
    val unit: String,
    val available: Int?,
    val capacity: Int,
    val policy: BookingPolicy,
    val reservable: Boolean,
    val needsExternalPayment: Boolean,
    val needsApproval: Boolean
) {
    fun totalFor(quantity: Int): BigDecimal {
        require(quantity > 0)
        return if (unit == "POR_CUPO") amount.multiply(BigDecimal(quantity)) else amount
    }
}

data class Booking(
    val reference: String,
    val confirmationCode: String,
    val serviceRef: String,
    val slotRef: String,
    val quantity: Int,
    val status: BookingStatus,
    val startsAt: Instant,
    val endsAt: Instant,
    val deadline: Instant?,
    val total: BigDecimal,
    val currency: String,
    val policy: BookingPolicy,
    val paymentInstructions: String?,
    val nextAction: String,
    val penaltyPercent: Int,
    val originalRef: String? = null,
    val replacementRef: String? = null,
    val externalAdjustment: BigDecimal? = null
) {
    fun canCancel(now: Instant): Boolean = status in setOf(
        BookingStatus.SOLICITADA,
        BookingStatus.PENDIENTE_PAGO_EXTERNO,
        BookingStatus.PENDIENTE_CONFIRMACION_PRESTADOR,
        BookingStatus.CONFIRMADA
    ) &&
        startsAt > now
}

data class BookingPage(val items: List<Booking>, val total: Int, val page: Int, val size: Int) {
    val hasNext: Boolean get() = (page.toLong() + 1) * size < total
}

data class BookingCommand(
    val serviceRef: String,
    val slotRef: String,
    val quantity: Int,
    val policyRef: String,
    val policyVersion: String,
    val accepted: Boolean
)

interface BookingRepository {
    fun clearLocalState()
    suspend fun offers(serviceRef: String): AppResult<List<BookingOffer>>
    suspend fun list(group: BookingGroup, page: Int): AppResult<BookingPage>
    suspend fun detail(reference: String): AppResult<Booking>
    suspend fun receipt(reference: String): AppResult<ByteArray>
    suspend fun modify(
        reference: String,
        command: BookingCommand,
        intent: String
    ): AppResult<Booking>
    suspend fun create(command: BookingCommand, intent: String): AppResult<Booking>
    suspend fun cancel(reference: String, reason: String, intent: String): AppResult<Booking>
}
