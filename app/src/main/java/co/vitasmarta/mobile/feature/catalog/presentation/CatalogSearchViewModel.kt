package co.vitasmarta.mobile.feature.catalog.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.catalog.domain.CatalogContentRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogMoney
import co.vitasmarta.mobile.feature.catalog.domain.CatalogOrder
import co.vitasmarta.mobile.feature.catalog.domain.CatalogPage
import co.vitasmarta.mobile.feature.catalog.domain.CatalogRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogSearchCriteria
import co.vitasmarta.mobile.feature.catalog.domain.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogSearchState(
    val query: String = "",
    val criteria: CatalogSearchCriteria = CatalogSearchCriteria(),
    val categories: List<Category> = emptyList(),
    val page: CatalogPage? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val queryError: Boolean = false,
    val filterError: Boolean = false,
    val error: AppError? = null,
    val moreError: AppError? = null
)

@HiltViewModel
class CatalogSearchViewModel @Inject constructor(
    private val repository: CatalogContentRepository,
    private val categoryRepository: CatalogRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(CatalogSearchState())
    val state = mutableState.asStateFlow()
    private var opened = false
    private var generation = 0L
    private var request: Job? = null
    private var debounce: Job? = null
    private val visitedCursors = mutableSetOf<String>()

    fun open(initialCategory: String?) {
        if (opened) return
        opened = true
        val categories = savedState.get<ArrayList<String>>("catalog_categories")?.toList()
            ?: listOfNotNull(initialCategory?.takeIf { it.isNotBlank() })
        val query = savedState.get<String>("catalog_query").orEmpty()
        val order = CatalogOrder.entries.firstOrNull {
            it.name == savedState.get<String>("catalog_order")
        } ?: CatalogOrder.Relevance
        val criteria = CatalogSearchCriteria(
            text = query.trim(),
            categories = categories,
            minimumPrice = savedState.get<String>("catalog_min")?.let(CatalogMoney::fromInput),
            maximumPrice = savedState.get<String>("catalog_max")?.let(CatalogMoney::fromInput),
            order = order,
            descending = savedState["catalog_descending"] ?: false
        )
        mutableState.value = mutableState.value.copy(query = query, criteria = criteria)
        viewModelScope.launch {
            val result = categoryRepository.categories()
            if (result is AppResult.Success) {
                mutableState.value = mutableState.value.copy(categories = result.value)
            }
        }
        submit()
    }

    fun queryChanged(value: String) {
        debounce?.cancel()
        request?.cancel()
        generation++
        mutableState.value = mutableState.value.copy(
            query = value,
            page = null,
            loading = value.length <= 200,
            loadingMore = false,
            queryError = false,
            error = null,
            moreError = null
        )
        savedState["catalog_query"] = value
        debounce = viewModelScope.launch {
            delay(400)
            if (mutableState.value.query.length <= 200) submit()
        }
    }

    fun submit() {
        debounce?.cancel()
        val current = mutableState.value
        if (current.query.length > 200) {
            mutableState.value = current.copy(queryError = true, loading = false)
            return
        }
        val criteria = current.criteria.copy(text = current.query.trim())
        savedState["catalog_query"] = current.query
        start(criteria)
    }

    fun filterEdited() {
        mutableState.value = mutableState.value.copy(filterError = false)
    }

    fun applyFilters(
        categories: List<String>,
        order: CatalogOrder,
        descending: Boolean,
        minimum: String,
        maximum: String
    ): Boolean {
        val min = minimum.takeIf { it.isNotBlank() }?.let(CatalogMoney::fromInput)
        val max = maximum.takeIf { it.isNotBlank() }?.let(CatalogMoney::fromInput)
        val invalid = (minimum.isNotBlank() && min == null) ||
            (maximum.isNotBlank() && max == null) ||
            (min != null && min.signum() < 0) ||
            (max != null && max.signum() < 0) ||
            (min != null && max != null && min > max)
        if (invalid) {
            mutableState.value = mutableState.value.copy(filterError = true)
            return false
        }
        savedState["catalog_categories"] = ArrayList(categories)
        savedState["catalog_order"] = order.name
        savedState["catalog_descending"] = descending
        savedState["catalog_min"] = min?.toPlainString()
        savedState["catalog_max"] = max?.toPlainString()
        mutableState.value = mutableState.value.copy(
            criteria = mutableState.value.criteria.copy(
                categories = categories.distinct(),
                order = order,
                descending = descending,
                minimumPrice = min,
                maximumPrice = max
            ),
            filterError = false
        )
        submit()
        return true
    }

    fun clearFilters() {
        mutableState.value = mutableState.value.copy(query = "")
        savedState["catalog_query"] = ""
        applyFilters(emptyList(), CatalogOrder.Relevance, false, "", "")
    }

    private fun start(criteria: CatalogSearchCriteria) {
        request?.cancel()
        val version = ++generation
        visitedCursors.clear()
        mutableState.value = mutableState.value.copy(
            criteria = criteria,
            page = null,
            loading = true,
            loadingMore = false,
            error = null,
            moreError = null,
            queryError = false
        )
        request = viewModelScope.launch {
            when (val result = repository.search(criteria)) {
                is AppResult.Success -> if (version == generation) {
                    mutableState.value = mutableState.value.copy(
                        page = result.value,
                        loading = false
                    )
                }
                is AppResult.Failure -> if (version == generation) {
                    mutableState.value = mutableState.value.copy(
                        error = result.error,
                        loading = false
                    )
                }
            }
        }
    }

    fun loadMore() {
        val current = mutableState.value
        val page = current.page ?: return
        val cursor = page.nextCursor ?: return
        if (current.loading || current.loadingMore) return
        if (cursor in visitedCursors) {
            mutableState.value = current.copy(
                page = page.copy(nextCursor = null),
                moreError = AppError(ErrorKind.InvalidResponse)
            )
            return
        }
        val version = generation
        mutableState.value = current.copy(loadingMore = true, moreError = null)
        request = viewModelScope.launch {
            when (val result = repository.search(current.criteria, cursor)) {
                is AppResult.Success -> if (version == generation) {
                    visitedCursors += cursor
                    val next = result.value
                    val repeated = next.nextCursor != null && next.nextCursor in visitedCursors
                    mutableState.value = mutableState.value.copy(
                        page = next.copy(
                            items = (page.items + next.items).distinctBy { it.id },
                            nextCursor = if (repeated) null else next.nextCursor
                        ),
                        loadingMore = false,
                        moreError = if (repeated) AppError(ErrorKind.InvalidResponse) else null
                    )
                }
                is AppResult.Failure -> if (version == generation) {
                    mutableState.value = mutableState.value.copy(
                        loadingMore = false,
                        moreError = result.error
                    )
                }
            }
        }
    }
}
