package co.vitasmarta.mobile.feature.catalog.domain

import java.math.BigDecimal

/** Bounded decimal parsing before any plain-string expansion or rendering. */
object CatalogMoney {
    private const val MAX_RAW_LENGTH = 64
    private const val MAX_PRECISION = 24
    private const val MAX_INTEGER_DIGITS = 16
    private const val MAX_SCALE = 8
    private val input = Regex("""[0-9]{1,16}(?:\.[0-9]{1,8})?""")
    private val remote = Regex(
        """[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE]([+-]?[0-9]{1,3}))?"""
    )

    fun fromInput(value: String): BigDecimal? {
        val text = value.trim()
        if (text.length !in 1..MAX_RAW_LENGTH || !input.matches(text)) return null
        return checked(text.toBigDecimalOrNull())
    }

    fun fromRemote(value: String): BigDecimal? {
        if (value.length !in 1..MAX_RAW_LENGTH) return null
        val match = remote.matchEntire(value) ?: return null
        val exponent = match.groups[1]?.value?.toIntOrNull()
        if (exponent != null && exponent !in -32..32) return null
        return checked(value.toBigDecimalOrNull())
    }

    fun isBounded(value: BigDecimal): Boolean {
        if (value.signum() < 0 ||
            value.precision() > MAX_PRECISION ||
            value.scale() !in -MAX_INTEGER_DIGITS..MAX_SCALE
        ) {
            return false
        }
        return value.precision().toLong() - value.scale().toLong() <= MAX_INTEGER_DIGITS
    }

    private fun checked(value: BigDecimal?): BigDecimal? {
        if (value == null || !isBounded(value)) return null
        // Scaling is safe only after the number of resulting integer digits is bounded.
        return if (value.scale() < 0) value.setScale(0) else value
    }
}
