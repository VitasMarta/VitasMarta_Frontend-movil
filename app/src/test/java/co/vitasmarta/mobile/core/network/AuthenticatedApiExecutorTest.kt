package co.vitasmarta.mobile.core.network

import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.testToken
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.auth.FakeIdentityApi
import co.vitasmarta.mobile.feature.auth.data.RemoteAuthRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class AuthenticatedApiExecutorTest {
    @Test fun retriesOnlyOnceAndKeepsTheCorrelationId() = runTest {
        val sessions = SessionStore()
        sessions.install(TokenPair(testToken(), "refresh"), 0)
        val api = FakeIdentityApi()
        val executor = ApiExecutor(Json)
        val authenticated =
            AuthenticatedApiExecutor(
                sessions,
                RemoteAuthRepository(api, sessions, executor),
                executor
            )
        val correlations = mutableListOf<String>()
        val result = authenticated.execute<String> { _, id ->
            correlations.add(id)
            Response.error(401, "{}".toResponseBody())
        } as AppResult.Failure
        assertEquals(ErrorKind.Unauthorized, result.error.kind)
        assertEquals(2, correlations.size)
        assertEquals(correlations[0], correlations[1])
        assertEquals(1, api.refreshCount)
        assertNull(sessions.snapshot().tokens)
    }

    @Test fun discardsPrivateDataReceivedAfterLogout() = runTest {
        val sessions = SessionStore()
        sessions.install(TokenPair(testToken(), "refresh"), 0)
        val executor = ApiExecutor(Json)
        val authenticated =
            AuthenticatedApiExecutor(
                sessions,
                RemoteAuthRepository(FakeIdentityApi(), sessions, executor),
                executor
            )
        val result = authenticated.execute { _, _ ->
            sessions.clear()
            Response.success("private-data")
        } as AppResult.Failure
        assertEquals(ErrorKind.Unauthorized, result.error.kind)
    }
}
