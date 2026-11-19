package co.vitasmarta.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.feature.auth.domain.RegistrationDetails
import co.vitasmarta.mobile.feature.auth.domain.RegistrationPasswordPolicy
import co.vitasmarta.mobile.feature.auth.domain.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegistrationState(
    val email: String = "",
    val password: String = "",
    val treatment: Boolean = false,
    val commercial: Boolean = false,
    val profiling: Boolean = false,
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val treatmentError: Boolean = false,
    val busy: Boolean = false,
    val created: Boolean = false,
    val retrySeconds: Long = 0,
    val error: AppError? = null
) {
    override fun toString(): String = "RegistrationState(busy=$busy, created=$created, redacted)"
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(private val repository: RegistrationRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(RegistrationState())
    val state = mutableState.asStateFlow()

    private fun edit(change: (RegistrationState) -> RegistrationState) {
        val current = mutableState.value
        if (!current.busy &&
            !current.created
        ) {
            mutableState.value = change(current).copy(error = null)
        }
    }

    fun emailChanged(value: String) = edit { it.copy(email = value, emailError = false) }
    fun passwordChanged(value: String) = edit { it.copy(password = value, passwordError = false) }
    fun treatmentChanged(value: Boolean) = edit {
        it.copy(treatment = value, treatmentError = false)
    }
    fun commercialChanged(value: Boolean) = edit { it.copy(commercial = value) }
    fun profilingChanged(value: Boolean) = edit { it.copy(profiling = value) }

    fun register() {
        val current = mutableState.value
        if (current.busy || current.created || current.retrySeconds > 0) return
        val checked = current.copy(
            emailError = !current.email.trim().matches(Regex("""[^\s@]+@[^\s@]+\.[^\s@]+""")),
            passwordError = !RegistrationPasswordPolicy.accepts(current.password),
            treatmentError = !current.treatment,
            error = null
        )
        mutableState.value = checked
        if (checked.emailError || checked.passwordError || checked.treatmentError) return
        mutableState.value = checked.copy(busy = true)
        viewModelScope.launch {
            try {
                when (
                    val result = repository.register(
                        RegistrationDetails(
                            current.email.trim(),
                            current.password,
                            current.treatment,
                            current.commercial,
                            current.profiling
                        )
                    )
                ) {
                    is AppResult.Success -> mutableState.value = RegistrationState(created = true)
                    is AppResult.Failure -> {
                        val seconds = result.error.retryAfterSeconds?.coerceAtLeast(0) ?: 0
                        mutableState.value = checked.copy(
                            password = "",
                            error = result.error,
                            retrySeconds = seconds
                        )
                        if (seconds > 0) countDown()
                    }
                }
            } finally {
                mutableState.value = mutableState.value.copy(password = "", busy = false)
            }
        }
    }

    private fun countDown() {
        viewModelScope.launch {
            while (mutableState.value.retrySeconds > 0) {
                delay(1000)
                mutableState.value = mutableState.value.copy(
                    retrySeconds = (mutableState.value.retrySeconds - 1).coerceAtLeast(0)
                )
            }
        }
    }
}
