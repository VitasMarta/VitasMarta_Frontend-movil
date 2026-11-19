package co.vitasmarta.mobile.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.preferences.AppearanceRepository
import co.vitasmarta.mobile.core.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AppearanceState(
    val mode: ThemeMode = ThemeMode.System,
    val ready: Boolean = false,
    val storageError: Boolean = false
)

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val repository: AppearanceRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(AppearanceState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.mode.catch { error ->
                if (error !is IOException) throw error
                mutableState.value = mutableState.value.copy(ready = true, storageError = true)
            }.collect { mode -> mutableState.value = AppearanceState(mode, ready = true) }
        }
    }

    fun select(mode: ThemeMode) {
        // Apply comfort changes even if persistence is unavailable.
        mutableState.value = AppearanceState(mode, ready = true)
        viewModelScope.launch {
            try {
                repository.setMode(mode)
            } catch (_: IOException) {
                mutableState.value = mutableState.value.copy(storageError = true)
            }
        }
    }
}
