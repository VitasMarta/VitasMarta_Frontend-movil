package co.vitasmarta.mobile.core.network

import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.Response

class ApiExecutor(private val json: Json) {
    suspend fun <T : Any> execute(call: suspend () -> Response<T>): AppResult<T> = try {
        val response = call()
        val body = response.body()
        when {
            !response.isSuccessful -> AppResult.Failure(mapError(response))
            body == null -> AppResult.Failure(AppError(ErrorKind.InvalidResponse))
            else -> AppResult.Success(body)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SocketTimeoutException) {
        AppResult.Failure(AppError(ErrorKind.Timeout))
    } catch (_: IOException) {
        AppResult.Failure(AppError(ErrorKind.Offline))
    } catch (_: SerializationException) {
        AppResult.Failure(AppError(ErrorKind.InvalidResponse))
    }

    private fun mapError(response: Response<*>): AppError {
        val raw = response.errorBody()?.use { it.string() }.orEmpty()
        val envelope = try {
            json.parseToJsonElement(raw) as? JsonObject
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        fun text(key: String): String? = (envelope?.get(key) as? JsonPrimitive)?.contentOrNull
        val fields = (envelope?.get("detalles") as? JsonArray)
            ?.mapNotNull { ((it as? JsonObject)?.get("campo") as? JsonPrimitive)?.contentOrNull }
            ?.toSet().orEmpty()
        val correlation =
            response.headers()["X-Correlation-Id"] ?: text("correlation_id") ?: text("trace_id")
        val kind = when (response.code()) {
            400, 422 -> ErrorKind.Validation
            401 -> ErrorKind.Unauthorized
            403 -> ErrorKind.Forbidden
            404 -> ErrorKind.NotFound
            409 -> ErrorKind.Conflict
            423 -> ErrorKind.Locked
            429 -> ErrorKind.RateLimited
            else -> ErrorKind.Server
        }
        return AppError(
            kind,
            correlation,
            retryAfterSeconds(response.headers()["Retry-After"]),
            fields
        )
    }
}
