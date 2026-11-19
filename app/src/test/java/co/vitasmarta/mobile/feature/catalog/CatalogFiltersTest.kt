package co.vitasmarta.mobile.feature.catalog

import co.vitasmarta.mobile.feature.catalog.domain.Category
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogFilters
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogFiltersTest {
    private val categories = listOf(
        Category("mountains", mapOf("es" to "Montaña", "en" to "Mountains"), true),
        Category("coast", mapOf("es" to "Costa", "en" to "Coast"), false)
    )

    @Test fun localSearchIsAccentInsensitiveAndPreservesOnlyPublishedData() {
        assertEquals(listOf(categories[0]), CatalogFilters(query = "  MONTANA ").apply(categories))
        assertEquals(listOf(categories[1]), CatalogFilters(query = "coast").apply(categories))
        assertEquals(emptyList<Category>(), CatalogFilters(query = "missing").apply(categories))
    }

    @Test fun primaryFilterUsesTheContractFlag() {
        assertEquals(listOf(categories[0]), CatalogFilters(primaryOnly = true).apply(categories))
        assertEquals(categories, CatalogFilters().apply(categories))
    }
}
