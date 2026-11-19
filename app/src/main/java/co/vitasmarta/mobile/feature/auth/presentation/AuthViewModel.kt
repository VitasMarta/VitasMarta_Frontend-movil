package co.vitasmarta.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val error: AppError? = null
) {
    override fun toString(): String = "LoginState(busy=$busy, redacted)"
}

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    val session = repository.session
    private val mutableState = MutableStateFlow(LoginState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            session.drop(1).collect { mutableState.value = LoginState() }
        }
    }

    fun emailChanged(value: String) {
        mutableState.value =
            mutableState.value.copy(email = value, emailError = false, error = null)
    }
    fun passwordChanged(value: String) {
        mutableState.value =
            mutableState.value.copy(password = value, passwordError = false, error = null)
    }

    fun login() {
        val current = mutableState.value
        if (current.busy) return
        val emailInvalid = !current.email.trim().matches(Regex("""[^\s@]+@[^\s@]+\.[^\s@]+"""))
        val passwordInvalid = current.password.isEmpty()
        mutableState.value =
            current.copy(emailError = emailInvalid, passwordError = passwordInvalid)
        if (emailInvalid || passwordInvalid) return
        mutableState.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                when (val result = repository.login(current.email.trim(), current.password)) {
                    is AppResult.Success -> mutableState.value = LoginState()
                    is AppResult.Failure ->
                        mutableState.value =
                            current.copy(password = "", error = result.error)
                }
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }

    fun logout() {
        if (mutableState.value.busy) return
        mutableState.value = LoginState(busy = true)
        viewModelScope.launch {
            try {
                val result = repository.logout()
                mutableState.value = LoginState(error = (result as? AppResult.Failure)?.error)
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }
}
