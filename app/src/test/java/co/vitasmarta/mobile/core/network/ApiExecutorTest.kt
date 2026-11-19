package co.vitasmarta.mobile.core.network

import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class ApiExecutorTest {
    private val executor = ApiExecutor(Json)

    @Test fun preservesDegradedSuccessAndRejectsMissingRequiredBody() = runTest {
        assertEquals(
            AppResult.Success("degraded"),
            executor.execute {
                Response.success(206, "degraded")
            }
        )
        val result = executor.execute<String> { Response.success(null) } as AppResult.Failure
        assertEquals(ErrorKind.InvalidResponse, result.error.kind)
    }

    @Test fun distinguishesAccessAndRecoversFromMalformedErrorEnvelopes() = runTest {
        val cases = mapOf(
            400 to ErrorKind.Validation,
            401 to ErrorKind.Unauthorized,
            403 to ErrorKind.Forbidden,
            409 to ErrorKind.Conflict,
            423 to ErrorKind.Locked,
            429 to ErrorKind.RateLimited,
            503 to ErrorKind.Server
        )
        for ((status, kind) in cases) {
            val result = executor.execute<String> {
                Response.error(status, "not json".toResponseBody("text/plain".toMediaType()))
            } as AppResult.Failure
            assertEquals(kind, result.error.kind)
        }
    }

    @Test fun extractsOnlyFieldNamesWithoutExposingBackendMessages() = runTest {
        val result = executor.execute<String> {
            Response.error(
                422,
                """{"mensaje":"private value","correlation_id":"support-id","detalles":[{"campo":"correo"},{"campo":{}},null]}"""
                    .toResponseBody("application/json".toMediaType())
            )
        } as AppResult.Failure
        assertEquals(setOf("correo"), result.error.fields)
        assertEquals("support-id", result.error.correlationId)
        assertFalse(result.toString().contains("private value"))
    }

    @Test fun distinguishesTimeoutFromOfflineAndPropagatesCancellation() = runTest {
        assertEquals(
            ErrorKind.Offline,
            (
                executor.execute<String> {
                    throw IOException()
                } as AppResult.Failure
                ).error.kind
        )
        assertEquals(
            ErrorKind.Timeout,
            (
                executor.execute<String> {
                    throw SocketTimeoutException()
                } as AppResult.Failure
                ).error.kind
        )
        val cancelled = CancellationException("cancelled")
        try {
            executor.execute<String> { throw cancelled }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }
}
