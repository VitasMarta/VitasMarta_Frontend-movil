package co.vitasmarta.mobile.feature.booking

import co.vitasmarta.mobile.core.auth.SessionSnapshot
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.TokenRefresher
import co.vitasmarta.mobile.core.auth.testToken
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.core.network.AuthenticatedApiExecutor
import co.vitasmarta.mobile.feature.booking.domain.Booking
import co.vitasmarta.mobile.feature.booking.domain.BookingCommand
import co.vitasmarta.mobile.feature.booking.domain.BookingOffer
import co.vitasmarta.mobile.feature.booking.domain.BookingPolicy
import co.vitasmarta.mobile.feature.booking.domain.BookingStatus
import java.math.BigDecimal
import java.time.Instant
import kotlinx.serialization.json.Json

internal const val BOOKING_JSON = """{
  "reserva_ref":"booking-1","codigo_confirmacion":"ABC123",
  "servicio_ref":"service-1","franja_ref":"slot-1","cantidad_cupos":2,
  "estado":"PENDIENTE_PAGO_EXTERNO","fecha_inicio":"2099-01-01T12:00:00Z",
  "fecha_fin":"2099-01-01T14:00:00Z","vence_en":"2098-12-31T12:00:00Z",
  "tarifa":{"importe_total":"60000.10","moneda":"COP"},
  "politica":{"politica_ref":"policy-1","version":"v1","contenido":"Terms from M3"},
  "instrucciones_pago_externo":"Contact provider","siguiente_accion":"CONTACTAR_PRESTADOR",
  "penalidad_aplicada_porcentaje":0,"original_ref":null,"reemplazo_ref":null,"ajuste_externo":null
}"""
internal const val OFFERS_JSON = """{
  "servicio_ref":"service-1","ofertas":[{
    "servicio_ref":"service-1","franja_ref":"slot-1",
    "inicio":"2099-01-01T12:00:00Z","fin":"2099-01-01T14:00:00Z",
    "importe":"30000.05","moneda":"COP","unidad_tarifa":"POR_CUPO",
    "capacidad_total":10,"disponibles":null,"reservable":true,
    "requiere_aprobacion":false,"requiere_pago_externo":true,
    "politica":{"politica_ref":"policy-1","version":"v1","contenido":"Terms from M3"}
  }]
}"""
internal val bookingCommand = BookingCommand("service-1", "slot-1", 2, "policy-1", "v1", true)
internal val bookingPolicy = BookingPolicy("policy-1", "v1", "Terms from M3")
internal val bookingOffer = BookingOffer(
    "service-1", "slot-1", Instant.parse("2099-01-01T12:00:00Z"),
    Instant.parse("2099-01-01T14:00:00Z"), BigDecimal("30000.05"), "COP", "POR_CUPO",
    10, 10, bookingPolicy, true, true, false
)
internal val testBooking = Booking(
    "booking-1", "ABC123", "service-1", "slot-1", 2,
    BookingStatus.PENDIENTE_PAGO_EXTERNO, bookingOffer.startsAt, bookingOffer.endsAt,
    Instant.parse("2098-12-31T12:00:00Z"), BigDecimal("60000.10"), "COP", bookingPolicy,
    "Contact provider", "CONTACTAR_PRESTADOR", 0
)
internal fun bookingSessions(): SessionStore = SessionStore().apply {
    check(install(TokenPair(testToken(), "refresh-test"), 0))
}
internal fun authenticated(sessions: SessionStore) = AuthenticatedApiExecutor(
    sessions,
    object : TokenRefresher {
        override suspend fun refresh(snapshot: SessionSnapshot): AppResult<Unit> =
            AppResult.Failure(AppError(ErrorKind.Unauthorized))
    },
    ApiExecutor(Json)
)
