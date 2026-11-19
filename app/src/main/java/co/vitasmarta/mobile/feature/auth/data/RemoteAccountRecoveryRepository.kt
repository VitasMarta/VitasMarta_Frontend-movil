package co.vitasmarta.mobile.feature.auth.data

import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.auth.domain.AccountRecoveryRepository

class RemoteAccountRecoveryRepository(
    private val api: IdentityApi?,
    private val executor: ApiExecutor
) : AccountRecoveryRepository {
    override suspend fun recover(email: String): AppResult<Unit> {
        val remote = api ?: return AppResult.Failure(AppError(ErrorKind.Configuration))
        return executor.execute { remote.recover(RecoveryRequest(email)) }
    }
}
