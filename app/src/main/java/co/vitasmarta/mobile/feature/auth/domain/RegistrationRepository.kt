package co.vitasmarta.mobile.feature.auth.domain

import co.vitasmarta.mobile.core.common.AppResult

class RegistrationDetails(
    val email: String,
    val password: String,
    val treatment: Boolean,
    val commercial: Boolean,
    val profiling: Boolean
) {
    override fun toString(): String = "RegistrationDetails(redacted)"
}

interface RegistrationRepository {
    suspend fun register(details: RegistrationDetails): AppResult<Unit>
}
