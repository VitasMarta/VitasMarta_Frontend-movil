package co.vitasmarta.mobile.feature.recommendations

import co.vitasmarta.mobile.TestGateway
import co.vitasmarta.mobile.core.auth.SessionSnapshot
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.TokenRefresher
import co.vitasmarta.mobile.core.auth.testToken
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.core.network.AuthenticatedApiExecutor
import co.vitasmarta.mobile.feature.recommendations.data.RecommendationsApi
import co.vitasmarta.mobile.feature.recommendations.data.RemoteRecommendationsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRecommendationsRepositoryTest {
    private fun repository(
        gateway: TestGateway,
        authenticated: Boolean = true
    ): RemoteRecommendationsRepository {
        val sessions = SessionStore()
        if (authenticated) check(sessions.install(TokenPair(testToken(), "refresh"), 0))
        val refresher = object : TokenRefresher {
            override suspend fun refresh(snapshot: SessionSnapshot): AppResult<Unit> =
                AppResult.Failure(AppError(ErrorKind.Unauthorized))
        }
        return RemoteRecommendationsRepository(
            gateway.services.create(RecommendationsApi::class.java),
            AuthenticatedApiExecutor(sessions, refresher, ApiExecutor(Json))
        )
    }
    private fun body(items: String = "[]", degraded: Boolean = false) =
        """{"modo":"ESTANDAR","degradada_fallback":$degraded,"personalizada":false,"metadatos_ia":{"aviso_transparencia":"Selección con reglas","generado_por_ia":false},"estado_ambiental_verificado":false,"recomendaciones":$items}"""

    @Test fun mapsTheRealM6ShapeAndKeepsMissingMetricsUnknown() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            gateway.server.enqueue(
                MockResponse().setHeader(
                    "Content-Type",
                    "application/json"
                ).setBody(
                    body(
                        """[{"atractivo_id":"a1","nombre":"Destino real","explicacion":"Por sus características","ocupacion_estimada":null,"precio_minimo":12000.50,"alternativas_sugeridas":["a2"]}]"""
                    )
                )
            )
            val feed = (repository.recommendations() as AppResult.Success).value
            assertEquals("12000.50", feed.items.single().minimumPrice?.toPlainString())
            assertNull(feed.items.single().occupancy)
            assertEquals(listOf("a2"), feed.items.single().alternatives)
            assertFalse(feed.personalized)
            assertFalse(feed.environmentalStateVerified)
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/recomendaciones", request.path)
            assertTrue(request.getHeader("Authorization").orEmpty().startsWith("Bearer "))
        }
    }

    @Test fun partial206IsStillContentAndIsMarkedAsDegradedEvenForAnEmptyList() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            gateway.server.enqueue(
                MockResponse().setResponseCode(
                    206
                ).setHeader("Content-Type", "application/json").setBody(body())
            )
            val feed = (repository.recommendations() as AppResult.Success).value
            assertTrue(feed.degraded)
            assertTrue(feed.items.isEmpty())
        }
    }

    @Test fun refusesTheOldInventedItemsShapeAndDuplicateDestinations() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            val item = """{"atractivo_id":"a1","nombre":"Destino","explicacion":"Motivo"}"""
            for (value in listOf("""{"items":[]}""", body("[$item,$item]"))) {
                gateway.server.enqueue(
                    MockResponse().setHeader("Content-Type", "application/json").setBody(value)
                )
                assertEquals(
                    ErrorKind.InvalidResponse,
                    (repository.recommendations() as AppResult.Failure).error.kind
                )
                gateway.server.takeRequest()
            }
        }
    }

    @Test fun anAnonymousCallerCannotSendARecommendationRequest() = runTest {
        TestGateway().use { gateway ->
            val result = repository(gateway, false).recommendations()
            assertEquals(ErrorKind.Unauthorized, (result as AppResult.Failure).error.kind)
            assertEquals(0, gateway.server.requestCount)
        }
    }

    @Test fun optionalPriceIsUnknownOnlyWhenAbsentAndRejectsMalformedOrExtremeAmounts() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            fun item(price: String?): String {
                val content = buildJsonObject {
                    put("atractivo_id", "a1")
                    put("nombre", "Destino")
                    put("explicacion", "Motivo")
                    if (price != null) put("precio_minimo", Json.parseToJsonElement(price))
                }
                return "[$content]"
            }
            for (price in listOf(null, "null")) {
                gateway.server.enqueue(
                    MockResponse().setHeader("Content-Type", "application/json")
                        .setBody(body(item(price)))
                )
                val feed = (repository.recommendations() as AppResult.Success).value
                assertNull(feed.items.single().minimumPrice)
                gateway.server.takeRequest()
            }
            for ((price, expected) in listOf(
                "1e2" to "100",
                "0.00000001" to "0.00000001",
                "9999999999999999.12345678" to "9999999999999999.12345678"
            )) {
                gateway.server.enqueue(
                    MockResponse().setHeader("Content-Type", "application/json")
                        .setBody(body(item(price)))
                )
                val amount = (repository.recommendations() as AppResult.Success)
                    .value.items.single().minimumPrice
                assertEquals(expected, amount?.toPlainString())
                gateway.server.takeRequest()
            }
            for (price in listOf(
                "\"gratis\"", "true", "-1", "1e100000000", "1e-100000000",
                "0.000000001", "10000000000000000", "1e33", "1e-33",
                "\"" + "9".repeat(65) + "\""
            )) {
                gateway.server.enqueue(
                    MockResponse().setHeader("Content-Type", "application/json")
                        .setBody(body(item(price)))
                )
                val result = repository.recommendations() as AppResult.Failure
                assertEquals(price.take(32), ErrorKind.InvalidResponse, result.error.kind)
                gateway.server.takeRequest()
            }
        }
    }
}
