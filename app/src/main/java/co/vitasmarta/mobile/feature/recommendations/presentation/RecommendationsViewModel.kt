package co.vitasmarta.mobile.feature.recommendations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationFeed
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecommendationsState(
    val loading: Boolean = true,
    val feed: RecommendationFeed? = null,
    val error: AppError? = null
)

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val repository: RecommendationsRepository,
    private val sessions: SessionStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecommendationsState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    init {
        viewModelScope.launch {
            sessions.state.collect { session ->
                request?.cancel()
                mutableState.value = RecommendationsState(loading = false)
                if (session is SessionState.Authenticated) reload()
            }
        }
    }
    fun reload() {
        request?.cancel()
        mutableState.value = RecommendationsState()
        request = viewModelScope.launch {
            val generation = sessions.snapshot().generation
            val result = repository.recommendations()
            currentCoroutineContext().ensureActive()
            if (sessions.snapshot().generation != generation ||
                sessions.snapshot().tokens == null
            ) {
                return@launch
            }
            when (result) {
                is AppResult.Success ->
                    mutableState.value =
                        RecommendationsState(false, result.value)
                is AppResult.Failure ->
                    mutableState.value =
                        RecommendationsState(false, error = result.error)
            }
        }
    }
}
