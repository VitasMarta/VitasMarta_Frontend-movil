package co.vitasmarta.mobile.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayConfigurationTest {
    @Test fun missingConfigurationDoesNotInventAnOrigin() {
        assertNull(GatewayConfiguration.from("").baseUrl)
    }

    @Test fun requiresAnOriginWithoutCredentialsOrPrefix() {
        listOf(
            "ftp://gateway.example",
            "http://name:password@gateway.example/",
            "http://gateway.example/api/v1/",
            "http://gateway.example/?token=value",
            "https://name:password@gateway.example/",
            "https://gateway.example/api/v1/",
            "https://gateway.example/?token=value",
            "https://gateway.example/#fragment",
            "invalid"
        ).forEach { input ->
            assertTrue(input, runCatching { GatewayConfiguration.from(input) }.isFailure)
        }
        assertEquals(
            "https://gateway.example/",
            GatewayConfiguration.from("https://gateway.example").baseUrl.toString()
        )
    }

    @Test fun acceptsAnExplicitHttpOriginAndPort() {
        assertEquals(
            "http://gateway.example:8080/",
            GatewayConfiguration.from("http://gateway.example:8080").baseUrl.toString()
        )
    }
}
