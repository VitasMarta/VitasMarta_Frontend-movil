package co.vitasmarta.mobile.feature.auth

import co.vitasmarta.mobile.TestGateway
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.auth.data.RegistrationApi
import co.vitasmarta.mobile.feature.auth.data.RemoteRegistrationRepository
import co.vitasmarta.mobile.feature.auth.domain.RegistrationDetails
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRegistrationRepositoryTest {
    private val details =
        RegistrationDetails("test@example.invalid", "Correcta1234!", true, false, true)

    @Test fun sendsTheConfirmedM7PayloadWithoutBearerAndOnlyOnce() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json")
                    .setBody("""{"mensaje":"Cuenta creada"}""")
            )
            val repository = RemoteRegistrationRepository(
                gateway.services.create(RegistrationApi::class.java),
                ApiExecutor(Json)
            )
            assertTrue(repository.register(details) is AppResult.Success)
            val request = gateway.server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/identidad/cuentas", request.path)
            assertNull(request.getHeader("Authorization"))
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals(setOf("correo", "password", "tipo_usuario", "consentimientos"), body.keys)
            assertEquals(details.email, body.getValue("correo").jsonPrimitive.content)
            assertEquals(details.password, body.getValue("password").jsonPrimitive.content)
            assertEquals("TURISTA", body.getValue("tipo_usuario").jsonPrimitive.content)
            val consents = body.getValue("consentimientos").jsonArray.associate {
                val value = it.jsonObject
                assertEquals(setOf("finalidad", "otorgado"), value.keys)
                value.getValue("finalidad").jsonPrimitive.content to
                    value.getValue("otorgado").jsonPrimitive.boolean
            }
            assertEquals(
                mapOf("TRATAMIENTO" to true, "COMERCIAL" to false, "PERFILADO" to true),
                consents
            )
            assertEquals(1, gateway.server.requestCount)
        }
    }

    @Test fun rejectsUnconfirmedSuccessAndPropagatesServerErrorsWithoutRetrying() = runTest {
        TestGateway().use { gateway ->
            val repository = RemoteRegistrationRepository(
                gateway.services.create(RegistrationApi::class.java),
                ApiExecutor(Json)
            )
            for ((code, body, kind) in listOf(
                Triple(200, """{"mensaje":"Not created"}""", ErrorKind.InvalidResponse),
                Triple(201, """{"mensaje":""}""", ErrorKind.InvalidResponse),
                Triple(201, "{}", ErrorKind.InvalidResponse),
                Triple(204, "", ErrorKind.InvalidResponse),
                Triple(400, "{}", ErrorKind.Validation),
                Triple(409, "{}", ErrorKind.Conflict),
                Triple(429, "{}", ErrorKind.RateLimited),
                Triple(503, "{}", ErrorKind.Server)
            )) {
                gateway.server.enqueue(
                    MockResponse().setResponseCode(code).setBody(body)
                        .setHeader("Content-Type", "application/json").setHeader("Retry-After", "2")
                )
                val failure = repository.register(details) as AppResult.Failure
                assertEquals(kind, failure.error.kind)
                if (code == 429) assertEquals(2L, failure.error.retryAfterSeconds)
                gateway.server.takeRequest()
            }
            assertEquals(8, gateway.server.requestCount)
        }
    }

    @Test fun missingConfigurationDoesNotInventAServerOrClaimSuccess() = runTest {
        val result = RemoteRegistrationRepository(null, ApiExecutor(Json)).register(details)
        assertEquals(ErrorKind.Configuration, (result as AppResult.Failure).error.kind)
    }
}
