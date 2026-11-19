package co.vitasmarta.mobile.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

data class AppError(
    val kind: ErrorKind,
    val correlationId: String? = null,
    val retryAfterSeconds: Long? = null,
    val fields: Set<String> = emptySet()
)

enum class ErrorKind {
    AdditionalVerification,
    Configuration,
    Offline,
    Timeout,
    Validation,
    Unauthorized,
    Forbidden,
    NotFound,
    Locked,
    Conflict,
    RateLimited,
    Server,
    InvalidResponse
}
