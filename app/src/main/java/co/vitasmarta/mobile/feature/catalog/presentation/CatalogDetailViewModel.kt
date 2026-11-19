package co.vitasmarta.mobile.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.catalog.domain.CatalogContentRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogDetail
import co.vitasmarta.mobile.feature.catalog.domain.CatalogSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogDetailState(
    val loading: Boolean = true,
    val detail: CatalogDetail? = null,
    val error: AppError? = null,
    val slots: List<CatalogSlot> = emptyList(),
    val slotsLoading: Boolean = false,
    val slotsError: AppError? = null
)

@HiltViewModel
class CatalogDetailViewModel @Inject constructor(private val repository: CatalogContentRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(CatalogDetailState())
    val state = mutableState.asStateFlow()
    private var id: String? = null
    private var language: String = "es-CO"
    private var generation = 0L
    private var request: Job? = null

    fun open(id: String, language: String) {
        if (this.id == id && this.language == language) return
        this.id = id
        this.language = language
        reload()
    }

    fun reload() {
        val requestedId = id ?: return
        request?.cancel()
        val version = ++generation
        mutableState.value = CatalogDetailState()
        if (requestedId.isBlank()) {
            mutableState.value = CatalogDetailState(
                loading = false,
                error = AppError(ErrorKind.NotFound)
            )
            return
        }
        request = viewModelScope.launch {
            when (val result = repository.detail(requestedId, language)) {
                is AppResult.Failure -> if (generation == version) {
                    mutableState.value = CatalogDetailState(loading = false, error = result.error)
                }
                is AppResult.Success -> if (generation == version) {
                    mutableState.value = CatalogDetailState(
                        loading = false,
                        detail = result.value,
                        slots = result.value.slots,
                        slotsLoading = true
                    )
                    when (val slots = repository.slots(requestedId)) {
                        is AppResult.Success -> if (generation == version) {
                            mutableState.value = mutableState.value.copy(
                                slots = slots.value,
                                slotsLoading = false
                            )
                        }
                        is AppResult.Failure -> if (generation == version) {
                            mutableState.value = mutableState.value.copy(
                                slotsError = slots.error,
                                slotsLoading = false
                            )
                        }
                    }
                }
            }
        }
    }
}
