package co.vitasmarta.mobile.core.network

import java.io.IOException
import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

class GatewayBoundaryInterceptor(
    private val configuration: GatewayConfiguration,
    private val locale: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val origin = configuration.baseUrl ?: throw IOException("Gateway not configured")
        val request = chain.request()
        val url = request.url
        if (url.scheme != origin.scheme ||
            url.host != origin.host ||
            url.port != origin.port ||
            !url.encodedPath.startsWith("/api/v1/")
        ) {
            throw IOException("Request outside Gateway boundary")
        }
        val correlation = request.header("X-Correlation-Id") ?: UUID.randomUUID().toString()
        return chain.proceed(
            request.newBuilder()
                .header("Accept", request.header("Accept") ?: "application/json")
                .header("Accept-Language", locale())
                .header("X-Correlation-Id", correlation)
                .build()
        )
    }
}
