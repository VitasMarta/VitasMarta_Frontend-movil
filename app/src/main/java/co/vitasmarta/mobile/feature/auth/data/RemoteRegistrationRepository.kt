package co.vitasmarta.mobile.feature.auth.data

import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.auth.domain.RegistrationDetails
import co.vitasmarta.mobile.feature.auth.domain.RegistrationRepository

class RemoteRegistrationRepository(
    private val api: RegistrationApi?,
    private val executor: ApiExecutor
) : RegistrationRepository {
    override suspend fun register(details: RegistrationDetails): AppResult<Unit> {
        val remote = api ?: return AppResult.Failure(AppError(ErrorKind.Configuration))
        val request = RegistrationRequest(
            details.email,
            details.password,
            "TURISTA",
            listOf(
                RegistrationConsent("TRATAMIENTO", details.treatment),
                RegistrationConsent("COMERCIAL", details.commercial),
                RegistrationConsent("PERFILADO", details.profiling)
            )
        )
        var created = false
        return when (
            val result = executor.execute {
                remote.register(request).also { created = it.code() == 201 }
            }
        ) {
            is AppResult.Failure -> result
            is AppResult.Success -> if (created && result.value.mensaje.isNotBlank()) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError(ErrorKind.InvalidResponse))
            }
        }
    }
}
