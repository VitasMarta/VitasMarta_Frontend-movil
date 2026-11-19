package co.vitasmarta.mobile.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class GatewayConfiguration private constructor(val baseUrl: HttpUrl?) {
    companion object {
        fun from(value: String): GatewayConfiguration {
            if (value.isBlank()) return GatewayConfiguration(null)
            val url = requireNotNull(value.toHttpUrlOrNull()) { "Invalid Gateway origin" }
            require(
                url.scheme in setOf("http", "https") &&
                    url.username.isEmpty() &&
                    url.password.isEmpty() &&
                    url.encodedPath == "/" &&
                    url.query == null &&
                    url.fragment == null
            ) {
                "Gateway must be an HTTP or HTTPS origin"
            }
            return GatewayConfiguration(url)
        }
    }
}
