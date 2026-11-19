package co.vitasmarta.mobile.feature.auth

import co.vitasmarta.mobile.TestGateway
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.testToken
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.auth.data.IdentityApi
import co.vitasmarta.mobile.feature.auth.data.LoginRequest
import co.vitasmarta.mobile.feature.auth.data.RecoveryRequest
import co.vitasmarta.mobile.feature.auth.data.RefreshRequest
import co.vitasmarta.mobile.feature.auth.data.RemoteAccountRecoveryRepository
import co.vitasmarta.mobile.feature.auth.data.RemoteAuthRepository
import co.vitasmarta.mobile.feature.auth.data.TokenResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteAuthRepositoryTest {
    private val executor = ApiExecutor(Json)

    @Test fun loginUsesTheRealGatewayRouteAndSpanishContractKeys() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json")
                    .setBody(
                        """{"access_token":"${testToken()}","refresh_token":"refresh","expira_en":3600}"""
                    )
            )
            val sessions = SessionStore()
            val repository =
                RemoteAuthRepository(
                    gateway.services.create(IdentityApi::class.java),
                    sessions,
                    executor
                )
            assertTrue(
                repository.login("test@example.invalid", "test-password") is AppResult.Success
            )
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/identidad/sesiones", request.path)
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("test@example.invalid", body["correo"]?.jsonPrimitive?.content)
            assertEquals("test-password", body["password"]?.jsonPrimitive?.content)
            assertNull(request.getHeader("Authorization"))
            assertTrue(sessions.state.value is SessionState.Authenticated)
        }
    }

    @Test
    fun recoverySendsOnlyEmailAndAcceptsAnEmpty202Response() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(MockResponse().setResponseCode(202))
            val repository = RemoteAccountRecoveryRepository(
                gateway.services.create(IdentityApi::class.java),
                executor
            )
            assertTrue(repository.recover("test@example.invalid") is AppResult.Success)
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/identidad/password/recuperacion", request.path)
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals(setOf("correo"), body.keys)
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test
    fun invalidLoginAndMfaNeverCreateAnAuthenticatedSession() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setResponseCode(401)
                    .setHeader("Content-Type", "application/json").setBody("{}")
            )
            gateway.server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json")
                    .setBody("""{"mfa_requerido":true,"mfa_token":"challenge-only"}""")
            )
            val sessions = SessionStore()
            val repository = RemoteAuthRepository(
                gateway.services.create(IdentityApi::class.java),
                sessions,
                executor
            )
            assertEquals(
                ErrorKind.Unauthorized,
                (repository.login("test@example.invalid", "test") as AppResult.Failure).error.kind
            )
            assertEquals(
                ErrorKind.AdditionalVerification,
                (repository.login("test@example.invalid", "test") as AppResult.Failure).error.kind
            )
            assertNull(sessions.snapshot().tokens)
        }
    }

    @Test fun concurrentRefreshCallsRotateOnlyOnceEvenWhenAccessTokenIsUnchanged() = runTest {
        val sessions = SessionStore()
        sessions.install(TokenPair(testToken(), "old-refresh"), 0)
        val api = FakeIdentityApi()
        val repository = RemoteAuthRepository(api, sessions, executor)
        val snapshot = sessions.snapshot()
        val results = (1..10).map { async { repository.refresh(snapshot) } }.awaitAll()
        assertTrue(results.all { it is AppResult.Success })
        assertEquals(1, api.refreshCount)
        assertEquals("new-refresh", sessions.snapshot().tokens?.refreshToken)
    }

    @Test fun logoutWinsAgainstAnInflightRefresh() = runTest {
        val sessions = SessionStore()
        sessions.install(TokenPair(testToken(), "old-refresh"), 0)
        val gate = CompletableDeferred<Unit>()
        val api = FakeIdentityApi(gate = gate)
        val repository = RemoteAuthRepository(api, sessions, executor)
        val pending = async { repository.refresh(sessions.snapshot()) }
        runCurrent()
        repository.logout()
        gate.complete(Unit)
        pending.await()
        assertTrue(sessions.state.value is SessionState.Anonymous)
        assertNull(sessions.snapshot().tokens)
    }

    @Test fun refreshRejectionClearsSessionButTemporaryFailureDoesNot() = runTest {
        for (code in listOf(401, 503)) {
            val sessions = SessionStore()
            sessions.install(TokenPair(testToken(), "old-refresh"), 0)
            val repository = RemoteAuthRepository(FakeIdentityApi(code = code), sessions, executor)
            val result = repository.refresh(sessions.snapshot()) as AppResult.Failure
            assertEquals(
                if (code ==
                    401
                ) {
                    ErrorKind.Unauthorized
                } else {
                    ErrorKind.Server
                },
                result.error.kind
            )
            assertEquals(code == 401, sessions.state.value is SessionState.Anonymous)
        }
    }

    @Test fun logoutClearsLocallyWhenRevocationFails() = runTest {
        val sessions = SessionStore()
        sessions.install(TokenPair(testToken(), "refresh"), 0)
        val api = object : FakeIdentityApi() {
            override suspend fun logout(bearer: String): Response<Unit> =
                Response.error(503, "".toResponseBody())
        }
        assertTrue(RemoteAuthRepository(api, sessions, executor).logout() is AppResult.Failure)
        assertNull(sessions.snapshot().tokens)
    }
}

open class FakeIdentityApi(
    private val code: Int = 200,
    private val gate: CompletableDeferred<Unit>? = null
) : IdentityApi {
    override suspend fun recover(request: RecoveryRequest): Response<Unit> =
        Response.success(202, Unit)
    var refreshCount = 0
    override suspend fun login(request: LoginRequest): Response<TokenResponse> =
        Response.success(TokenResponse(testToken(), "refresh"))
    override suspend fun refresh(request: RefreshRequest): Response<TokenResponse> {
        refreshCount++
        if (gate != null) gate.await() else delay(10)
        return if (code == 200) {
            Response.success(TokenResponse(testToken(), "new-refresh"))
        } else {
            Response.error(code, "{}".toResponseBody("application/json".toMediaType()))
        }
    }
    override suspend fun logout(bearer: String): Response<Unit> = Response.success(Unit)
}
