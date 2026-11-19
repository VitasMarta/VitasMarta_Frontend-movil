package co.vitasmarta.mobile

import co.vitasmarta.mobile.core.network.GatewayBoundaryInterceptor
import co.vitasmarta.mobile.core.network.GatewayConfiguration
import co.vitasmarta.mobile.core.network.GatewayServices
import java.io.Closeable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate

class TestGateway : Closeable {
    val server = MockWebServer()
    val services: GatewayServices

    init {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverCertificates = HandshakeCertificates.Builder().heldCertificate(
            certificate
        ).build()
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(
            certificate.certificate
        ).build()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()
        val configuration = GatewayConfiguration.from(server.url("/").toString())
        val client = OkHttpClient.Builder().sslSocketFactory(
            trust.sslSocketFactory(),
            trust.trustManager
        )
            .followRedirects(false).addInterceptor(
                GatewayBoundaryInterceptor(configuration) {
                    "es-CO"
                }
            ).build()
        services = GatewayServices(configuration, client, Json { ignoreUnknownKeys = true })
    }
    override fun close() {
        server.shutdown()
    }
}
