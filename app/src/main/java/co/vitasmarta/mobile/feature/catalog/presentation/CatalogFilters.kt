package co.vitasmarta.mobile.feature.catalog.presentation

import co.vitasmarta.mobile.feature.catalog.domain.Category
import java.text.Normalizer
import java.util.Locale

data class CatalogFilters(val query: String = "", val primaryOnly: Boolean = false) {
    fun apply(categories: List<Category>): List<Category> {
        val text = normalized(query.trim())
        return categories.filter { category ->
            (!primaryOnly || category.topLevel) &&
                (
                    text.isEmpty() ||
                        category.names.values.any { normalized(it).contains(text) } ||
                        normalized(category.slug).contains(text)
                    )
        }
    }

    private fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "").lowercase(Locale.ROOT)
}
