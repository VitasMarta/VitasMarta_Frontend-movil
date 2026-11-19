package co.vitasmarta.mobile.core.network

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetryAfterTest {
    @Test
    fun handlesSecondsDatesAndMalformedValues() {
        val clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC)
        assertEquals(30L, retryAfterSeconds("30", clock))
        assertEquals(60L, retryAfterSeconds("Tue, 17 Nov 2026 12:01:00 GMT", clock))
        assertEquals(0L, retryAfterSeconds("Tue, 17 Nov 2026 11:00:00 GMT", clock))
        assertNull(retryAfterSeconds("-1", clock))
        assertNull(retryAfterSeconds("invalid", clock))
    }
}
