package co.vitasmarta.mobile.feature.auth.data

import co.vitasmarta.mobile.core.auth.SessionSnapshot
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.TokenRefresher
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteAuthRepository(
    private val api: IdentityApi?,
    private val sessions: SessionStore,
    private val executor: ApiExecutor
) : AuthRepository,
    TokenRefresher {
    override val session = sessions.state
    private val refreshMutex = Mutex()

    override suspend fun login(email: String, password: String): AppResult<Unit> {
        val remote = api ?: return failure(ErrorKind.Configuration)
        val generation = sessions.clear()
        return when (
            val result = executor.execute {
                remote.login(LoginRequest(email, password))
            }
        ) {
            is AppResult.Failure -> result
            is AppResult.Success -> install(result.value, generation)
        }
    }

    override suspend fun refresh(snapshot: SessionSnapshot): AppResult<Unit> =
        refreshMutex.withLock {
            val current = sessions.snapshot()
            if (current.generation != snapshot.generation ||
                current.tokens == null
            ) {
                return@withLock failure(ErrorKind.Unauthorized)
            }
            if (current.tokens !== snapshot.tokens) return@withLock AppResult.Success(Unit)
            val remote = api ?: return@withLock failure(ErrorKind.Configuration)
            when (
                val result = executor.execute {
                    remote.refresh(RefreshRequest(current.tokens.refreshToken))
                }
            ) {
                is AppResult.Failure -> {
                    if (result.error.kind ==
                        ErrorKind.Unauthorized
                    ) {
                        sessions.clearIfCurrent(snapshot.generation)
                    }
                    result
                }
                is AppResult.Success -> {
                    val installed = install(result.value, snapshot.generation)
                    if (installed is AppResult.Failure) sessions.clearIfCurrent(snapshot.generation)
                    installed
                }
            }
        }

    override suspend fun logout(): AppResult<Unit> {
        val token = sessions.snapshot().tokens
        sessions.clear()
        if (token == null) return AppResult.Success(Unit)
        val remote = api ?: return failure(ErrorKind.Configuration)
        return executor.execute { remote.logout("Bearer ${token.accessToken}") }
    }

    private fun install(response: TokenResponse, generation: Long): AppResult<Unit> {
        if (response.mfaRequired) return failure(ErrorKind.AdditionalVerification)
        val access = response.accessToken ?: return failure(ErrorKind.InvalidResponse)
        val refresh = response.refreshToken ?: return failure(ErrorKind.InvalidResponse)
        return if (sessions.install(
                TokenPair(access, refresh),
                generation
            )
        ) {
            AppResult.Success(Unit)
        } else {
            failure(ErrorKind.InvalidResponse)
        }
    }

    private fun failure(kind: ErrorKind) = AppResult.Failure(AppError(kind))
}
