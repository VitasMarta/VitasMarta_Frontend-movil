package co.vitasmarta.mobile.feature.catalog

import co.vitasmarta.mobile.feature.catalog.domain.CatalogMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMoneyTest {
    @Test fun ordinaryInputRejectsExponentsExtremeLengthAndUnsupportedPrecision() {
        for (input in listOf(
            "1e100000000",
            "1e2",
            "9".repeat(10_000),
            "12345678901234567",
            "0.123456789",
            "-1",
            "NaN",
            "Infinity"
        )) {
            assertNull(input.take(70), CatalogMoney.fromInput(input))
        }
        assertEquals("12345.67000000", CatalogMoney.fromInput("12345.67000000")?.toPlainString())
        assertEquals("0", CatalogMoney.fromInput("0")?.toPlainString())
    }

    @Test fun remoteNumbersHaveBoundsBeforeScientificNotationCanBeExpanded() {
        for (input in listOf(
            "1e100000000", "1e2147483647", "1e-2147483647", "1e33", "1e-9",
            "9".repeat(10_000), "12345678901234567.1", "-0.01", "not-a-price"
        )) {
            assertNull(input.take(70), CatalogMoney.fromRemote(input))
        }
        assertEquals("100", CatalogMoney.fromRemote("1e2")?.toPlainString())
        assertEquals("0.00000001", CatalogMoney.fromRemote("1e-8")?.toPlainString())
        assertEquals(
            "1234567890123456.12345678",
            CatalogMoney.fromRemote("1234567890123456.12345678")?.toPlainString()
        )
    }
}
