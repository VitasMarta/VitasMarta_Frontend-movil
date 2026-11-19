package co.vitasmarta.mobile.feature.catalog

import co.vitasmarta.mobile.TestGateway
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.catalog.data.CatalogApi
import co.vitasmarta.mobile.feature.catalog.data.RemoteCatalogContentRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogOrder
import co.vitasmarta.mobile.feature.catalog.domain.CatalogSearchCriteria
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogContentRepositoryTest {
    @Test fun searchSendsCategoriesCursorAndFiltersWithoutCredentials() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setBody(
                    """{"items":[{"id":"place-1","nombre":{"es":"Playa"},"categorias":["mar"],"precio_minimo":123.45}],"total":3,"cursor_siguiente":"next","desde_cache":true}"""
                )
                    .setHeader("Content-Type", "application/json")
            )
            val repository = RemoteCatalogContentRepository(
                gateway.services.create(CatalogApi::class.java),
                ApiExecutor(Json)
            )
            val result = repository.search(
                CatalogSearchCriteria(
                    "  playa  ",
                    listOf("mar", "aventura"),
                    BigDecimal("1.20"),
                    BigDecimal("500.99"),
                    CatalogOrder.Price,
                    true
                ),
                "opaque+/="
            ) as AppResult.Success
            assertEquals("Playa", result.value.items.single().displayName("es-CO"))
            assertEquals(BigDecimal("123.45"), result.value.items.single().minimumPrice)
            assertEquals("next", result.value.nextCursor)
            assertTrue(result.value.fromCache)
            val request = gateway.server.takeRequest()
            val url = requireNotNull(request.requestUrl)
            assertEquals("/api/v1/catalogo/busqueda", url.encodedPath)
            assertEquals(listOf("mar", "aventura"), url.queryParameterValues("categorias"))
            assertEquals("playa", url.queryParameter("texto"))
            assertEquals("opaque+/=", url.queryParameter("cursor"))
            assertEquals("1.20", url.queryParameter("precio_minimo"))
            assertEquals("500.99", url.queryParameter("precio_maximo"))
            assertEquals("precio", url.queryParameter("orden"))
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test fun detailKeepsMissingValuesUnknownAndOrdersSafePublicMedia() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json")
                    .setBody(
                        """{"id":"place-1","nombre":"Lugar","atributos":{"duracion":"2 horas","ausente":null},"multimedia":[{"tipo":"foto","url_cdn":"https://images.example/two.jpg","orden":2},{"tipo":"foto","url_cdn":"https://images.example/one.jpg","orden":1},{"tipo":"foto","url_cdn":"https://name:secret@images.example/no.jpg","orden":0}]}"""
                    )
            )
            val repository = RemoteCatalogContentRepository(
                gateway.services.create(CatalogApi::class.java),
                ApiExecutor(Json)
            )
            val value = (repository.detail("place-1", "es-CO") as AppResult.Success).value
            assertNull(value.description)
            assertNull(value.minimumPrice)
            assertNull(value.location)
            assertFalse(value.freshnessWarning)
            assertEquals(listOf(1, 2), value.media.map { it.order })
            assertEquals("duracion", value.attributes.single().name)
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/catalogo/place-1", request.requestUrl?.encodedPath)
            assertEquals("es-CO", request.requestUrl?.queryParameter("idioma"))
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test fun invalidSchemasAndMismatchedDetailNeverAppearAsEmptySuccess() = runTest {
        TestGateway().use { gateway ->
            val repository = RemoteCatalogContentRepository(
                gateway.services.create(CatalogApi::class.java),
                ApiExecutor(Json)
            )
            for (body in listOf(
                """{"items":[{}],"total":1}""",
                """{"items":[],"total":-1}""",
                """{"items":[{"id":"p","nombre":{"es":"P"},"categorias":[],"precio_minimo":"bad"}],"total":1}"""
            )) {
                gateway.server.enqueue(MockResponse().setBody(body))
                val failure = repository.search(CatalogSearchCriteria()) as AppResult.Failure
                assertEquals(ErrorKind.InvalidResponse, failure.error.kind)
            }
            gateway.server.enqueue(MockResponse().setBody("""{"id":"wrong"}"""))
            assertEquals(
                ErrorKind.InvalidResponse,
                (repository.detail("place-1", "es-CO") as AppResult.Failure).error.kind
            )
        }
    }

    @Test fun slotsUseTheirConfirmedEndpointAndValidateInformationalDates() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setBody(
                    """{"id":"p","franjas":[{"fecha":"2099-01-01","hora_inicio":"09:00:00","hora_fin":"10:00:00","cupo":10,"estado":"ACTIVA","disponibles":null}]}"""
                )
            )
            val repository = RemoteCatalogContentRepository(
                gateway.services.create(CatalogApi::class.java),
                ApiExecutor(Json)
            )
            val value = (repository.slots("p") as AppResult.Success).value.single()
            assertNull(value.available)
            assertEquals(10, value.capacity)
            assertEquals("/api/v1/catalogo/p/franjas", gateway.server.takeRequest().path)
        }
    }

    @Test fun remoteExponentsAndOversizedPricesAreInvalidResponsesWithoutExpansion() = runTest {
        TestGateway().use { gateway ->
            val repository = RemoteCatalogContentRepository(
                gateway.services.create(CatalogApi::class.java),
                ApiExecutor(Json)
            )
            for (price in listOf("1e100000000", "1e-100000000", "9".repeat(65))) {
                gateway.server.enqueue(
                    MockResponse().setBody(
                        """{"items":[{"id":"p","nombre":{"es":"P"},"categorias":[],"precio_minimo":$price}],"total":1}"""
                    )
                )
                val result = repository.search(CatalogSearchCriteria()) as AppResult.Failure
                assertEquals(ErrorKind.InvalidResponse, result.error.kind)
            }
        }
    }
}
