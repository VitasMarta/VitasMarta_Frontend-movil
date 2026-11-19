package co.vitasmarta.mobile.feature.profile

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
import co.vitasmarta.mobile.feature.profile.data.ProfileApi
import co.vitasmarta.mobile.feature.profile.data.ProfileCategoriesApi
import co.vitasmarta.mobile.feature.profile.data.RemoteProfileRepository
import co.vitasmarta.mobile.feature.profile.domain.RightsRequest
import co.vitasmarta.mobile.feature.profile.domain.RightsType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProfileRepositoryTest {
    private fun repository(gateway: TestGateway): RemoteProfileRepository {
        val sessions = SessionStore()
        check(sessions.install(TokenPair(testToken(), "refresh"), 0))
        val executor = ApiExecutor(Json)
        val refresher = object : TokenRefresher {
            override suspend fun refresh(snapshot: SessionSnapshot): AppResult<Unit> =
                AppResult.Failure(AppError(ErrorKind.Unauthorized))
        }
        return RemoteProfileRepository(
            gateway.services.create(ProfileApi::class.java),
            AuthenticatedApiExecutor(sessions, refresher, executor),
            gateway.services.create(ProfileCategoriesApi::class.java),
            executor
        )
    }
    private fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json").setBody(body)

    @Test fun profileUsesTheM7NameContractAndDoesNotOverwriteContactData() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            gateway.server.enqueue(
                json(
                    """{"usuario_ref":"u1","nombre_visible":"Ana","datos_contacto":{"presupuesto":"medio"}}"""
                )
            )
            assertEquals("Ana", (repository.profile() as AppResult.Success).value.visibleName)
            val read = gateway.server.takeRequest()
            assertEquals("/api/v1/identidad/usuarios/actual/perfil", read.path)
            assertTrue(read.getHeader("Authorization").orEmpty().startsWith("Bearer "))
            assertFalse(read.getHeader("X-Correlation-Id").isNullOrBlank())
            gateway.server.enqueue(json("""{"mensaje":"El perfil fue actualizado"}"""))
            assertTrue(repository.updateName("Ana María") is AppResult.Success)
            val request = gateway.server.takeRequest()
            assertEquals("PUT", request.method)
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals(setOf("nombre_visible"), body.keys)
            assertEquals("Ana María", body.getValue("nombre_visible").jsonPrimitive.content)
        }
    }

    @Test fun languageOnlyWritesPreferredLanguageAndUsesTheConfirmedResponse() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            gateway.server.enqueue(
                json(
                    """{"idioma_preferido":"en-US","idioma_autorizado":"es-CO","canal_autorizado":"CORREO"}"""
                )
            )
            val value = (repository.updateLanguage("en-US") as AppResult.Success).value
            assertEquals("en-US", value.preferredLanguage)
            assertEquals("es-CO", value.mailLanguage)
            assertEquals("CORREO", value.channel)
            val request = gateway.server.takeRequest()
            assertEquals("/api/v1/identidad/usuarios/actual/preferencias", request.path)
            assertEquals(
                setOf("idioma_preferido"),
                Json.parseToJsonElement(request.body.readUtf8()).jsonObject.keys
            )
            assertTrue(repository.updateLanguage("unsupported") is AppResult.Failure)
            assertEquals(1, gateway.server.requestCount)
        }
    }

    @Test fun optionalConsentIsIndependentAndMissingEvidenceDoesNotGrantPermission() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            gateway.server.enqueue(
                json(
                    """{"vigente":{"COMERCIAL":null,"PERFILADO":{"otorgado":false,"version_politica":"1.0"}},"historial":[]}"""
                )
            )
            val current = (repository.consents() as AppResult.Success).value.current
            assertNull(current["COMERCIAL"])
            assertFalse(current.getValue("PERFILADO")!!.granted)
            gateway.server.takeRequest()
            gateway.server.enqueue(json("""{"mensaje":"Confirmado"}"""))
            assertTrue(repository.setConsent("PERFILADO", true) is AppResult.Success)
            val request = gateway.server.takeRequest()
            assertEquals(
                "/api/v1/identidad/usuarios/actual/consentimientos/PERFILADO",
                request.path
            )
            assertEquals("""{"otorgado":true}""", request.body.readUtf8())
            assertTrue(repository.setConsent("TRATAMIENTO", false) is AppResult.Failure)
            assertEquals(2, gateway.server.requestCount)
        }
    }

    @Test fun categoriesArePublicAndInterestsAreExplicitReplacementWithoutInventedGet() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            gateway.server.enqueue(json("""[{"slug":"cultura","nombre":{"es":"Cultura"}}]"""))
            assertEquals(
                "Cultura",
                (repository.categories() as AppResult.Success).value.single().displayName("es-CO")
            )
            val categories = gateway.server.takeRequest()
            assertEquals("/api/v1/catalogo/categorias", categories.path)
            assertNull(categories.getHeader("Authorization"))
            gateway.server.enqueue(json("""{"mensaje":"Los intereses fueron actualizados (0)"}"""))
            assertTrue(repository.replaceInterests(emptyList()) is AppResult.Success)
            val replacement = gateway.server.takeRequest()
            assertEquals("PUT", replacement.method)
            assertEquals("/api/v1/identidad/usuarios/actual/intereses", replacement.path)
            assertEquals("""{"intereses":[]}""", replacement.body.readUtf8())
        }
    }

    @Test fun rightsSubmissionUsesExactPayloadAndRequiresA201Receipt() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            for (type in RightsType.entries) {
                gateway.server.enqueue(
                    json("""{"solicitud_ref":"case-1","mensaje":"Registrada"}""", 201)
                )
                val value = when (type) {
                    RightsType.RECTIFICACION -> "Nuevo nombre"
                    RightsType.OPOSICION -> "PERFILADO"
                    else -> ""
                }
                assertTrue(repository.submitRights(RightsRequest(type, value)) is AppResult.Success)
                val request = gateway.server.takeRequest()
                val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
                assertEquals("/api/v1/identidad/usuarios/actual/solicitudes-arco", request.path)
                assertEquals(type.name, body.getValue("tipo").jsonPrimitive.content)
                val detail = body.getValue("detalle").jsonObject
                when (type) {
                    RightsType.RECTIFICACION -> assertEquals(
                        setOf("campo", "valor_nuevo"),
                        detail.keys
                    )
                    RightsType.OPOSICION -> assertEquals(setOf("finalidad"), detail.keys)
                    else -> assertTrue(detail.isEmpty())
                }
            }
            gateway.server.enqueue(json("""{"solicitud_ref":"case-2","mensaje":"Registrada"}"""))
            assertEquals(
                ErrorKind.InvalidResponse,
                (
                    repository.submitRights(
                        RightsRequest(RightsType.ACCESO)
                    ) as AppResult.Failure
                    ).error.kind
            )
        }
    }

    @Test fun rightsHistoryAndStepsComeFromTheirActualPrivateEndpoints() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            val body = """
                {"solicitud_ref":"case-1","tipo":"ACCESO","estado":"EVIDENCIADA","fecha_recepcion":"2026-11-18T12:00:00Z","pasos":[{"paso":"RESPUESTA","resultado":"Confirmado"}]}
            """.trimIndent()
            gateway.server.enqueue(json("[$body]"))
            assertEquals(1, (repository.rightsCases() as AppResult.Success).value.size)
            gateway.server.takeRequest()
            gateway.server.enqueue(json(body))
            val detail = (repository.rightsCase("case-1") as AppResult.Success).value
            assertEquals("RESPUESTA", detail.steps.single().name)
            assertEquals(
                "/api/v1/identidad/usuarios/actual/solicitudes-arco/case-1",
                gateway.server.takeRequest().path
            )
        }
    }

    @Test fun exportAcceptsOnlySmallPdfResponsesAndNeverReturnsHtmlAsAPdf() = runTest {
        TestGateway().use { gateway ->
            val repository = repository(gateway)
            gateway.server.enqueue(
                MockResponse().setHeader(
                    "Content-Type",
                    "application/pdf"
                ).setBody("%PDF-1.4\nexample")
            )
            assertTrue(repository.exportData() is AppResult.Success)
            val request = gateway.server.takeRequest()
            assertEquals("application/pdf", request.getHeader("Accept"))
            assertEquals("/api/v1/identidad/usuarios/actual/exportacion-datos", request.path)
            for ((type, body) in listOf(
                "text/html" to "<html>error</html>",
                "application/pdf" to "not a pdf",
                "application/pdf" to ("%PDF-" + "x".repeat(10 * 1024 * 1024))
            )) {
                gateway.server.enqueue(MockResponse().setHeader("Content-Type", type).setBody(body))
                assertEquals(
                    ErrorKind.InvalidResponse,
                    (repository.exportData() as AppResult.Failure).error.kind
                )
                gateway.server.takeRequest()
            }
        }
    }
}
