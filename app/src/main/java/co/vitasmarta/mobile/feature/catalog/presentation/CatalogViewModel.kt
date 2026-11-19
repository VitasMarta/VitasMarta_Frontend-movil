package co.vitasmarta.mobile.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.feature.catalog.domain.CatalogRepository
import co.vitasmarta.mobile.feature.catalog.domain.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Content(val categories: List<Category>) : CatalogState
    data class Error(val error: AppError) : CatalogState
}

@HiltViewModel
class CatalogViewModel @Inject constructor(private val repository: CatalogRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    private val mutableFilters = MutableStateFlow(CatalogFilters())
    val filters = mutableFilters.asStateFlow()
    val state = combine(mutableState, mutableFilters) { source, filters ->
        if (source is CatalogState.Content) {
            source.copy(categories = filters.apply(source.categories))
        } else {
            source
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CatalogState.Loading)

    fun queryChanged(value: String) {
        mutableFilters.value =
            mutableFilters.value.copy(query = value)
    }
    fun primaryChanged(value: Boolean) {
        mutableFilters.value =
            mutableFilters.value.copy(primaryOnly = value)
    }
    private var loading = false

    init {
        reload()
    }

    fun reload() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            mutableState.value = CatalogState.Loading
            try {
                mutableState.value = when (val result = repository.categories()) {
                    is AppResult.Success -> CatalogState.Content(result.value)
                    is AppResult.Failure -> CatalogState.Error(result.error)
                }
            } finally {
                loading = false
            }
        }
    }
}
