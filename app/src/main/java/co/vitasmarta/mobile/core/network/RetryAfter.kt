package co.vitasmarta.mobile.core.network

import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal fun retryAfterSeconds(value: String?, clock: Clock = Clock.systemUTC()): Long? {
    val raw = value?.trim() ?: return null
    raw.toLongOrNull()?.let { return it.takeIf { seconds -> seconds >= 0 } }
    return try {
        val until = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
        val millis = Duration.between(clock.instant(), until).toMillis().coerceAtLeast(0)
        millis / 1000 + if (millis % 1000 == 0L) 0 else 1
    } catch (_: DateTimeParseException) {
        null
    }
}
