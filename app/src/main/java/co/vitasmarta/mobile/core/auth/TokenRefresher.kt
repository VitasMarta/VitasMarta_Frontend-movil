package co.vitasmarta.mobile.core.auth

import co.vitasmarta.mobile.core.common.AppResult

interface TokenRefresher {
    suspend fun refresh(snapshot: SessionSnapshot): AppResult<Unit>
}
