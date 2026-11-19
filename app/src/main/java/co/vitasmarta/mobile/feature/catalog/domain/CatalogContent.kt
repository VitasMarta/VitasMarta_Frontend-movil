package co.vitasmarta.mobile.feature.catalog.domain

import co.vitasmarta.mobile.core.common.AppResult
import java.math.BigDecimal

// M2 sorting values, not display labels.
enum class CatalogOrder(val wire: String) {
    Relevance("relevancia"),
    Name("nombre"),
    Price("precio"),
    Popularity("popularidad"),
    Sustainability("sostenibilidad")
}

data class CatalogSearchCriteria(
    val text: String = "",
    val categories: List<String> = emptyList(),
    val minimumPrice: BigDecimal? = null,
    val maximumPrice: BigDecimal? = null,
    val order: CatalogOrder = CatalogOrder.Relevance,
    val descending: Boolean = false
) {
    val filtered: Boolean get() = text.isNotBlank() ||
        categories.isNotEmpty() ||
        minimumPrice != null ||
        maximumPrice != null
}

data class CatalogItem(
    val id: String,
    val names: Map<String, String>,
    val categories: List<String>,
    val minimumPrice: BigDecimal? = null,
    val maximumPrice: BigDecimal? = null,
    val imageUrl: String? = null
) {
    fun displayName(locale: String): String = localizedName(names, locale, id)
}

data class CatalogPage(
    val items: List<CatalogItem>,
    val total: Long,
    val nextCursor: String?,
    val fromCache: Boolean,
    val lastUpdated: String?
)

data class CatalogLocation(val latitude: Double, val longitude: Double)
data class CatalogMedia(val kind: String, val url: String, val order: Int)
data class CatalogAttribute(val name: String, val value: String)
data class CatalogSlot(
    val id: String?,
    val date: String,
    val start: String,
    val end: String,
    val capacity: Int,
    val status: String,
    val available: Int? = null,
    val validUntil: String? = null,
    val withdrawn: Boolean = false
)

data class CatalogDetail(
    val id: String,
    val name: String?,
    val description: String?,
    val categories: List<String>,
    val location: CatalogLocation?,
    val attributes: List<CatalogAttribute>,
    val media: List<CatalogMedia>,
    val minimumPrice: BigDecimal?,
    val maximumPrice: BigDecimal?,
    val currency: String?,
    val slots: List<CatalogSlot>,
    val fromCache: Boolean,
    val freshnessWarning: Boolean,
    val lastUpdated: String?
)

interface CatalogContentRepository {
    suspend fun search(
        criteria: CatalogSearchCriteria,
        cursor: String? = null
    ): AppResult<CatalogPage>
    suspend fun detail(id: String, language: String): AppResult<CatalogDetail>
    suspend fun slots(id: String): AppResult<List<CatalogSlot>>
}

internal fun localizedName(names: Map<String, String>, locale: String, fallback: String): String =
    listOf(locale, locale.substringBefore('-'), "es-CO", "es", "en-US", "en")
        .firstNotNullOfOrNull { names[it]?.takeIf(String::isNotBlank) }
        ?: names.values.firstOrNull(String::isNotBlank) ?: fallback
