package co.vitasmarta.mobile.feature.booking

import co.vitasmarta.mobile.TestGateway
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.booking.data.BookingApi
import co.vitasmarta.mobile.feature.booking.data.BookingDto
import co.vitasmarta.mobile.feature.booking.data.BookingPageDto
import co.vitasmarta.mobile.feature.booking.data.CancelBookingDto
import co.vitasmarta.mobile.feature.booking.data.CreateBookingDto
import co.vitasmarta.mobile.feature.booking.data.KeyDto
import co.vitasmarta.mobile.feature.booking.data.KeyRequest
import co.vitasmarta.mobile.feature.booking.data.OffersDto
import co.vitasmarta.mobile.feature.booking.data.RemoteBookingRepository
import co.vitasmarta.mobile.feature.booking.domain.BookingGroup
import co.vitasmarta.mobile.feature.booking.domain.BookingStatus
import java.math.BigDecimal
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteBookingRepositoryTest {
    @Test fun offersPreserveUnknownAvailabilityExactMoneyAndAuthentication() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(jsonResponse(OFFERS_JSON))
            val sessions = bookingSessions()
            val repository = RemoteBookingRepository(
                gateway.services.create(BookingApi::class.java),
                authenticated(sessions),
                sessions
            )
            val offers = (repository.offers("service-1") as AppResult.Success).value
            assertNull(offers.single().available)
            assertEquals(BigDecimal("60000.10"), offers.single().totalFor(2))
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/reservas/ofertas/service-1", request.path)
            assertTrue(request.getHeader("Authorization")?.startsWith("Bearer ") == true)
            assertTrue(request.getHeader("X-Correlation-Id")?.isNotBlank() == true)
        }
    }

    @Test fun createUsesAnIssuedKeyAndReturnsTheSameReceiptForDuplicateIntent() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                jsonResponse("""{"clave_idempotencia":"server-key","operacion":"iniciar"}""", 201)
            )
            gateway.server.enqueue(jsonResponse(BOOKING_JSON, 201))
            val sessions = bookingSessions()
            val repository = RemoteBookingRepository(
                gateway.services.create(BookingApi::class.java),
                authenticated(sessions),
                sessions
            )
            val created = repository.create(bookingCommand, "intent-1") as AppResult.Success
            assertEquals(BookingStatus.PENDIENTE_PAGO_EXTERNO, created.value.status)
            val keyRequest = gateway.server.takeRequest()
            assertEquals("/api/v1/reservas/claves-idempotencia", keyRequest.path)
            assertEquals(
                "iniciar",
                Json.parseToJsonElement(keyRequest.body.readUtf8())
                    .jsonObject.getValue("operacion").jsonPrimitive.content
            )
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/reservas", request.path)
            assertEquals("server-key", request.getHeader("Idempotency-Key"))
            assertEquals(
                keyRequest.getHeader("X-Correlation-Id"),
                request.getHeader("X-Correlation-Id")
            )
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals(
                setOf("servicio_ref", "franja_ref", "cantidad_cupos", "aceptacion_politica"),
                body.keys
            )
            assertEquals(
                "policy-1",
                body.getValue("aceptacion_politica")
                    .jsonObject.getValue("politica_ref").jsonPrimitive.content
            )
            assertTrue(repository.create(bookingCommand, "intent-1") is AppResult.Success)
            assertEquals(2, gateway.server.requestCount)
            assertEquals(
                ErrorKind.Validation,
                (
                    repository.create(
                        bookingCommand.copy(quantity = 3),
                        "intent-1"
                    ) as AppResult.Failure
                    ).error.kind
            )
            assertEquals(2, gateway.server.requestCount)
        }
    }

    @Test fun timeoutRetainsTheIssuedKeyAndCorrelationForAnExplicitRetry() = runTest {
        val sessions = bookingSessions()
        val api = object : StubBookingApi() {
            var issued = 0
            val keys = mutableListOf<String>()
            val correlations = mutableListOf<String>()
            override suspend fun issueKey(
                request: KeyRequest,
                bearer: String,
                correlation: String
            ): Response<KeyDto> {
                issued++
                return Response.success(201, KeyDto("retained-key", "iniciar"))
            }
            override suspend fun create(
                request: CreateBookingDto,
                key: String,
                bearer: String,
                correlation: String
            ): Response<BookingDto> {
                keys += key
                correlations += correlation
                if (keys.size == 1) throw SocketTimeoutException("Lost response")
                return Response.success(201, Json.decodeFromString<BookingDto>(BOOKING_JSON))
            }
        }
        val repository = RemoteBookingRepository(api, authenticated(sessions), sessions)
        assertEquals(
            ErrorKind.Timeout,
            (repository.create(bookingCommand, "intent") as AppResult.Failure).error.kind
        )
        assertTrue(repository.create(bookingCommand, "intent") is AppResult.Success)
        assertEquals(1, api.issued)
        assertEquals(listOf("retained-key", "retained-key"), api.keys)
        assertEquals(api.correlations.first(), api.correlations.last())
    }

    @Test fun modificationUsesItsOwnScopeAndAcceptsTheReplacementReference() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                jsonResponse(
                    """{"clave_idempotencia":"modify-key","operacion":"modificar:original-1"}""",
                    201
                )
            )
            gateway.server.enqueue(
                jsonResponse(
                    BOOKING_JSON.replace("\"original_ref\":null", "\"original_ref\":\"original-1\"")
                )
            )
            val sessions = bookingSessions()
            val repository = RemoteBookingRepository(
                gateway.services.create(BookingApi::class.java),
                authenticated(sessions),
                sessions
            )
            val replacement = repository.modify(
                "original-1",
                bookingCommand,
                "modify-intent"
            ) as AppResult.Success
            assertEquals("booking-1", replacement.value.reference)
            assertEquals("original-1", replacement.value.originalRef)
            gateway.server.takeRequest()
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/reservas/original-1/modificar", request.path)
            assertEquals("modify-key", request.getHeader("Idempotency-Key"))
        }
    }

    @Test fun cancellationUsesTheTypedReasonAndRequiresACancelledResponse() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                jsonResponse(
                    """{"clave_idempotencia":"cancel-key","operacion":"cancelar:booking-1"}""",
                    201
                )
            )
            gateway.server.enqueue(
                jsonResponse(BOOKING_JSON.replace("PENDIENTE_PAGO_EXTERNO", "CANCELADA"))
            )
            val sessions = bookingSessions()
            val repository = RemoteBookingRepository(
                gateway.services.create(BookingApi::class.java),
                authenticated(sessions),
                sessions
            )
            assertEquals(
                BookingStatus.CANCELADA,
                (
                    repository.cancel(
                        "booking-1",
                        " Changed plans ",
                        "cancel-intent"
                    ) as AppResult.Success
                    ).value.status
            )
            gateway.server.takeRequest()
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/reservas/booking-1/cancelar", request.path)
            assertEquals("cancel-key", request.getHeader("Idempotency-Key"))
            assertEquals(
                "Changed plans",
                Json.parseToJsonElement(request.body.readUtf8())
                    .jsonObject.getValue("motivo").jsonPrimitive.content
            )
        }
    }

    @Test fun listRejectsWrongPageAndMalformedMoneyInsteadOfInventingContent() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                jsonResponse("""{"content":[],"total_elements":0,"page":1,"size":20}""")
            )
            gateway.server.enqueue(jsonResponse(OFFERS_JSON.replace("30000.05", "NaN")))
            val sessions = bookingSessions()
            val repository = RemoteBookingRepository(
                gateway.services.create(BookingApi::class.java),
                authenticated(sessions),
                sessions
            )
            assertEquals(
                ErrorKind.InvalidResponse,
                (repository.list(BookingGroup.ACTIVAS, 0) as AppResult.Failure).error.kind
            )
            assertEquals(
                ErrorKind.InvalidResponse,
                (repository.offers("service-1") as AppResult.Failure).error.kind
            )
        }
    }

    @Test fun extremeDecimalExponentsAreRejectedBeforeReachingMoneyFormatters() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(jsonResponse(OFFERS_JSON.replace("30000.05", "1e100000000")))
            gateway.server.enqueue(jsonResponse(BOOKING_JSON.replace("60000.10", "1e100000000")))
            val sessions = bookingSessions()
            val repository = RemoteBookingRepository(
                gateway.services.create(BookingApi::class.java),
                authenticated(sessions),
                sessions
            )
            assertEquals(
                ErrorKind.InvalidResponse,
                (repository.offers("service-1") as AppResult.Failure).error.kind
            )
            assertEquals(
                ErrorKind.InvalidResponse,
                (repository.detail("booking-1") as AppResult.Failure).error.kind
            )
        }
    }

    @Test fun pdfIsPrivateAndRejectsNonPdfOrOversizedBodies() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setHeader(
                    "Content-Type",
                    "application/pdf"
                ).setBody("%PDF-1.7\nprivate receipt")
            )
            gateway.server.enqueue(
                MockResponse().setHeader("Content-Type", "application/pdf").setBody("not a pdf")
            )
            gateway.server.enqueue(
                MockResponse().setHeader("Content-Type", "text/html").setBody("%PDF-1.7")
            )
            val large = ByteArray(10 * 1024 * 1024 + 1) { 65 }
            gateway.server.enqueue(
                MockResponse().setHeader(
                    "Content-Type",
                    "application/pdf"
                ).setBody(Buffer().write(large))
            )
            val sessions = bookingSessions()
            val repository = RemoteBookingRepository(
                gateway.services.create(BookingApi::class.java),
                authenticated(sessions),
                sessions
            )
            assertTrue(repository.receipt("booking-1") is AppResult.Success)
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/reservas/booking-1/comprobante", request.path)
            assertTrue(request.getHeader("Authorization")?.startsWith("Bearer ") == true)
            repeat(3) {
                assertEquals(
                    ErrorKind.InvalidResponse,
                    (repository.receipt("booking-1") as AppResult.Failure).error.kind
                )
            }
        }
    }

    @Test fun logoutDuringKeyIssuePreventsTheMutation() = runTest {
        val sessions = bookingSessions()
        val api = object : StubBookingApi() {
            var creates = 0
            override suspend fun issueKey(
                request: KeyRequest,
                bearer: String,
                correlation: String
            ): Response<KeyDto> {
                sessions.clear()
                return Response.success(201, KeyDto("key", "iniciar"))
            }
            override suspend fun create(
                request: CreateBookingDto,
                key: String,
                bearer: String,
                correlation: String
            ): Response<BookingDto> {
                creates++
                return Response.success(201, Json.decodeFromString<BookingDto>(BOOKING_JSON))
            }
        }
        val repository = RemoteBookingRepository(api, authenticated(sessions), sessions)
        assertEquals(
            ErrorKind.Unauthorized,
            (repository.create(bookingCommand, "intent") as AppResult.Failure).error.kind
        )
        assertEquals(0, api.creates)
        assertEquals(
            ErrorKind.Unauthorized,
            (repository.create(bookingCommand, "intent") as AppResult.Failure).error.kind
        )
        assertFalse(sessions.snapshot().tokens != null)
    }

    @Test fun aCompletedIntentReceiptIsNotReusedAfterAnotherSessionStarts() = runTest {
        val sessions = bookingSessions()
        val api = object : StubBookingApi() {
            var issued = 0
            var created = 0
            override suspend fun issueKey(
                request: KeyRequest,
                bearer: String,
                correlation: String
            ): Response<KeyDto> {
                issued++
                return Response.success(201, KeyDto("key-$issued", "iniciar"))
            }
            override suspend fun create(
                request: CreateBookingDto,
                key: String,
                bearer: String,
                correlation: String
            ): Response<BookingDto> {
                created++
                return Response.success(
                    201,
                    Json.decodeFromString<BookingDto>(
                        BOOKING_JSON.replace("booking-1", "booking-$created")
                    )
                )
            }
        }
        val repository = RemoteBookingRepository(api, authenticated(sessions), sessions)
        assertEquals(
            "booking-1",
            (repository.create(bookingCommand, "same-intent") as AppResult.Success).value.reference
        )
        val generation = sessions.clear()
        assertTrue(
            sessions.install(
                co.vitasmarta.mobile.core.auth.TokenPair(
                    co.vitasmarta.mobile.core.auth.testToken(),
                    "new-refresh"
                ),
                generation
            )
        )
        assertEquals(
            "booking-2",
            (repository.create(bookingCommand, "same-intent") as AppResult.Success).value.reference
        )
        assertEquals(2, api.issued)
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setHeader("Content-Type", "application/json").setResponseCode(code).setBody(body)
}

internal open class StubBookingApi : BookingApi {
    private fun <T> unavailable(): Response<T> = Response.error(503, "{}".toResponseBody())
    override suspend fun offers(
        service: String,
        bearer: String,
        correlation: String
    ): Response<OffersDto> = unavailable()
    override suspend fun list(
        group: String,
        page: Int,
        size: Int,
        bearer: String,
        correlation: String
    ): Response<BookingPageDto> = unavailable()
    override suspend fun detail(
        reference: String,
        bearer: String,
        correlation: String
    ): Response<BookingDto> = unavailable()
    override suspend fun issueKey(
        request: KeyRequest,
        bearer: String,
        correlation: String
    ): Response<KeyDto> = unavailable()
    override suspend fun create(
        request: CreateBookingDto,
        key: String,
        bearer: String,
        correlation: String
    ): Response<BookingDto> = unavailable()
    override suspend fun cancel(
        reference: String,
        request: CancelBookingDto,
        key: String,
        bearer: String,
        correlation: String
    ): Response<BookingDto> = unavailable()
    override suspend fun receipt(
        reference: String,
        bearer: String,
        correlation: String
    ): Response<ResponseBody> = unavailable()
    override suspend fun modify(
        reference: String,
        request: CreateBookingDto,
        key: String,
        bearer: String,
        correlation: String
    ): Response<BookingDto> = unavailable()
}
