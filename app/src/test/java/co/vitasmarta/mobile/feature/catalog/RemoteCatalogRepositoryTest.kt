package co.vitasmarta.mobile.feature.catalog

import co.vitasmarta.mobile.TestGateway
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.catalog.data.CatalogApi
import co.vitasmarta.mobile.feature.catalog.data.RemoteCatalogRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteCatalogRepositoryTest {
    @Test fun loadsPublicCategoriesAndPreservesTheirLocalizedNames() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json")
                    .setBody(
                        """[{"slug":"nature","nombre":{"es-CO":"Naturaleza","en-US":"Nature"},"primer_nivel":true}]"""
                    )
            )
            val repository =
                RemoteCatalogRepository(
                    gateway.services.create(CatalogApi::class.java),
                    ApiExecutor(Json)
                )
            val result = repository.categories() as AppResult.Success
            assertEquals("Naturaleza", result.value.single().displayName("es-CO"))
            assertEquals("Nature", result.value.single().displayName("en-US"))
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/catalogo/categorias", request.path)
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test fun missingConfigurationIsDifferentFromAnEmptyCatalog() = runTest {
        val result = RemoteCatalogRepository(
            null,
            ApiExecutor(Json)
        ).categories() as AppResult.Failure
        assertEquals(ErrorKind.Configuration, result.error.kind)
    }

    @Test fun schemaFailuresAreNotReportedAsEmptyResults() = runTest {
        TestGateway().use { gateway ->
            gateway.server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json").setBody("[{}]")
            )
            val result = RemoteCatalogRepository(
                gateway.services.create(CatalogApi::class.java),
                ApiExecutor(Json)
            ).categories() as AppResult.Failure
            assertEquals(ErrorKind.InvalidResponse, result.error.kind)
        }
    }
}
