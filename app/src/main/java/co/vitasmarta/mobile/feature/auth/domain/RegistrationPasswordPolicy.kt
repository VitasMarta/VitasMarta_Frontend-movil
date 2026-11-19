package co.vitasmarta.mobile.feature.auth.domain

object RegistrationPasswordPolicy {
    // Mirrors M7 PoliticaPassword and RegistroRequest; the server remains authoritative.
    fun accepts(value: String): Boolean = value.length in 12..128 &&
        value.any(Char::isUpperCase) &&
        value.any(Char::isLowerCase) &&
        value.any(Char::isDigit) &&
        value.any { !it.isLetterOrDigit() && !it.isWhitespace() }
}
