package co.vitasmarta.mobile.feature.auth.domain

import co.vitasmarta.mobile.core.common.AppResult

interface AccountRecoveryRepository {
    suspend fun recover(email: String): AppResult<Unit>
}
