package co.vitasmarta.mobile.feature.catalog

import co.vitasmarta.mobile.feature.catalog.domain.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {
    @Test
    fun supportsLanguageKeysObservedInTheGateway() {
        val category = Category("nature", mapOf("es" to "Naturaleza", "en" to "Nature"), true)
        assertEquals("Naturaleza", category.displayName("es-CO"))
        assertEquals("Nature", category.displayName("en-US"))
        assertEquals("Naturaleza", category.displayName("fr-FR"))
    }
}
