package co.vitasmarta.mobile.core.network

import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenRefresher
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import java.util.UUID
import javax.inject.Inject
import retrofit2.Response

class AuthenticatedApiExecutor @Inject constructor(
    private val sessions: SessionStore,
    private val refresher: TokenRefresher,
    private val executor: ApiExecutor
) {
    suspend fun <T : Any> execute(
        call: suspend (bearer: String, correlationId: String) -> Response<T>
    ): AppResult<T> {
        val snapshot = sessions.snapshot()
        val token = snapshot.tokens ?: return AppResult.Failure(AppError(ErrorKind.Unauthorized))
        val correlation = UUID.randomUUID().toString()
        val first = executor.execute { call("Bearer ${token.accessToken}", correlation) }
        if (sessions.snapshot().generation !=
            snapshot.generation
        ) {
            return AppResult.Failure(AppError(ErrorKind.Unauthorized))
        }
        if (first !is AppResult.Failure || first.error.kind != ErrorKind.Unauthorized) return first
        val refreshed = refresher.refresh(snapshot)
        if (refreshed is AppResult.Failure) return refreshed
        val current = sessions.snapshot()
        if (current.generation !=
            snapshot.generation
        ) {
            return AppResult.Failure(AppError(ErrorKind.Unauthorized))
        }
        val updated = current.tokens ?: return AppResult.Failure(AppError(ErrorKind.Unauthorized))
        val result = executor.execute { call("Bearer ${updated.accessToken}", correlation) }
        if (sessions.snapshot().generation !=
            snapshot.generation
        ) {
            return AppResult.Failure(AppError(ErrorKind.Unauthorized))
        }
        if (result is AppResult.Failure &&
            result.error.kind == ErrorKind.Unauthorized
        ) {
            sessions.clearIfCurrent(snapshot.generation)
        }
        return result
    }
}
