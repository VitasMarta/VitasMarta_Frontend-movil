package co.vitasmarta.mobile.feature.auth.domain

import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.common.AppResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<SessionState>
    suspend fun login(email: String, password: String): AppResult<Unit>
    suspend fun logout(): AppResult<Unit>
}
