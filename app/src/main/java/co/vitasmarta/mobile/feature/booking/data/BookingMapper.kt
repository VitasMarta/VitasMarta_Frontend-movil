package co.vitasmarta.mobile.feature.booking.data

import co.vitasmarta.mobile.feature.booking.domain.Booking
import co.vitasmarta.mobile.feature.booking.domain.BookingOffer
import co.vitasmarta.mobile.feature.booking.domain.BookingPolicy
import co.vitasmarta.mobile.feature.booking.domain.BookingStatus
import java.math.BigDecimal
import java.time.Instant

internal fun PolicyDto.domain(): BookingPolicy {
    require(reference.isNotBlank() && version.isNotBlank() && contenido.isNotBlank())
    require(cancellationHours == null || cancellationHours >= 0)
    require(penaltyPercent == null || penaltyPercent in 0..100)
    return BookingPolicy(reference, version, contenido, cancellationHours, penaltyPercent)
}

internal fun OfferDto.domain(): BookingOffer {
    val start = Instant.parse(inicio)
    val end = Instant.parse(fin)
    val amount = boundedMoney(importe)
    require(serviceRef.isNotBlank() && slotRef.isNotBlank() && end > start)
    require(moneda == "COP" && amount.signum() >= 0 && unit in setOf("POR_CUPO", "POR_RESERVA"))
    require(capacity >= 0 && (disponibles == null || disponibles in 0..capacity))
    return BookingOffer(
        serviceRef, slotRef, start, end, amount, moneda, unit, disponibles, capacity,
        politica.domain(), reservable, needsExternalPayment, needsApproval
    )
}

internal fun BookingDto.domain(): Booking {
    val start = Instant.parse(startsAt)
    val end = Instant.parse(endsAt)
    val amount = boundedMoney(tarifa.total)
    require(reference.isNotBlank() && serviceRef.isNotBlank() && slotRef.isNotBlank())
    require(confirmationCode.isNotBlank() && quantity > 0 && end > start)
    require(tarifa.moneda == "COP" && amount.signum() >= 0 && penaltyPercent in 0..100)
    return Booking(
        reference, confirmationCode, serviceRef, slotRef, quantity,
        BookingStatus.valueOf(estado), start, end, deadline?.let(Instant::parse),
        amount, tarifa.moneda, politica.domain(), paymentInstructions, nextAction, penaltyPercent,
        originalRef, replacementRef,
        externalAdjustment?.let {
            boundedMoney(it, nonnegative = false)
        }
    )
}

// Bound metadata before any formatter can expand a compact, hostile exponent.
private fun boundedMoney(raw: String, nonnegative: Boolean = true): BigDecimal {
    require(raw.length in 1..64)
    val exponent = raw.substringAfter('e', raw.substringAfter('E', ""))
    if (exponent.isNotEmpty()) {
        require(exponent.matches(Regex("[+-]?[0-9]{1,3}")))
        require(exponent.toInt() in -32..32)
    }
    val value = BigDecimal(raw)
    require(!nonnegative || value.signum() >= 0)
    require(value.scale() in -16..8)
    require(value.precision() <= 24)
    require(value.precision() - value.scale() <= 16)
    return if (value.scale() < 0) value.setScale(0) else value
}
