package co.vitasmarta.mobile.core.network

import java.io.IOException
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class GatewayBoundaryInterceptorTest {
    @Test fun sendsLocaleAndRetainsCorrelationInsideTheHttpsBoundary() {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverCertificates = HandshakeCertificates.Builder().heldCertificate(
            certificate
        ).build()
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(
            certificate.certificate
        ).build()
        MockWebServer().use { server ->
            server.useHttps(serverCertificates.sslSocketFactory(), false)
            server.start()
            val origin = GatewayConfiguration.from(server.url("/").toString())
            val client = OkHttpClient.Builder().sslSocketFactory(
                trust.sslSocketFactory(),
                trust.trustManager
            )
                .addInterceptor(GatewayBoundaryInterceptor(origin) { "en-US" }).build()
            server.enqueue(MockResponse().setBody("{}"))
            val id = UUID.randomUUID().toString()
            client.newCall(
                Request.Builder().url(server.url("/api/v1/catalogo/categorias"))
                    .header("X-Correlation-Id", id).build()
            ).execute().use {
                assertEquals(200, it.code)
            }
            val recorded = server.takeRequest()
            assertEquals(id, recorded.getHeader("X-Correlation-Id"))
            assertEquals("en-US", recorded.getHeader("Accept-Language"))
            assertNull(recorded.getHeader("Authorization"))
            for (url in listOf(
                server.url("/bff/api/v1/catalogo/categorias"),
                server.url("/api/v1/catalogo/categorias").newBuilder().scheme("http").build(),
                server.url("/api/v1/catalogo/categorias").newBuilder().host("127.0.0.1").build()
            )) {
                try {
                    client.newCall(Request.Builder().url(url).build()).execute().close()
                    fail("Boundary must reject $url")
                } catch (_: IOException) { /* Rejected before any request leaves the client. */ }
            }
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun httpRemainsRestrictedToConfiguredSchemeHostPortAndApiPrefix() {
        MockWebServer().use { server ->
            server.start()
            val origin = GatewayConfiguration.from(server.url("/").toString())
            val client = OkHttpClient.Builder()
                .addInterceptor(GatewayBoundaryInterceptor(origin) { "es-CO" }).build()
            server.enqueue(MockResponse().setBody("{}"))
            client.newCall(
                Request.Builder().url(server.url("/api/v1/catalogo/categorias")).build()
            ).execute().use { assertEquals(200, it.code) }
            val recorded = server.takeRequest()
            assertEquals("es-CO", recorded.getHeader("Accept-Language"))
            assertNull(recorded.getHeader("Authorization"))
            UUID.fromString(recorded.getHeader("X-Correlation-Id"))
            for (url in listOf(
                server.url("/bff/api/v1/catalogo/categorias"),
                server.url("/api/v1/catalogo/categorias").newBuilder().scheme("https").build(),
                server.url("/api/v1/catalogo/categorias").newBuilder().host("127.0.0.1").build(),
                server.url("/api/v1/catalogo/categorias").newBuilder().port(server.port + 1).build()
            )) {
                try {
                    client.newCall(Request.Builder().url(url).build()).execute().close()
                    fail("Boundary must reject requests outside the configured origin")
                } catch (_: IOException) { /* Rejected before leaving the client. */ }
            }
            assertEquals(1, server.requestCount)
        }
    }
}
