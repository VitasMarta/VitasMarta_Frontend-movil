package co.vitasmarta.mobile.feature.catalog.domain

import co.vitasmarta.mobile.core.common.AppResult

data class Category(val slug: String, val names: Map<String, String>, val topLevel: Boolean) {
    fun displayName(locale: String): String = names[locale]
        ?: names[locale.substringBefore('-')]
        ?: names["es-CO"] ?: names["es"]
        ?: names["en-US"] ?: names["en"] ?: slug
}

interface CatalogRepository {
    suspend fun categories(): AppResult<List<Category>>
}
