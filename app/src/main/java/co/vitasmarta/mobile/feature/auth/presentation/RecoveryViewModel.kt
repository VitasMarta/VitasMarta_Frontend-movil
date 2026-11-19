package co.vitasmarta.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.feature.auth.domain.AccountRecoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecoveryState(
    val email: String = "",
    val invalidEmail: Boolean = false,
    val busy: Boolean = false,
    val accepted: Boolean = false,
    val error: AppError? = null
) {
    override fun toString(): String = "RecoveryState(busy=$busy, accepted=$accepted, redacted)"
}

@HiltViewModel
class RecoveryViewModel @Inject constructor(private val repository: AccountRecoveryRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(RecoveryState())
    val state = mutableState.asStateFlow()

    fun emailChanged(value: String) {
        mutableState.value = RecoveryState(email = value)
    }

    fun recover() {
        val current = mutableState.value
        if (current.busy || current.accepted) return
        if (!current.email.trim().matches(Regex("""[^\s@]+@[^\s@]+\.[^\s@]+"""))) {
            mutableState.value = current.copy(invalidEmail = true)
            return
        }
        mutableState.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                mutableState.value = when (val result = repository.recover(current.email.trim())) {
                    is AppResult.Success -> RecoveryState(accepted = true)
                    is AppResult.Failure -> current.copy(error = result.error)
                }
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }
}
