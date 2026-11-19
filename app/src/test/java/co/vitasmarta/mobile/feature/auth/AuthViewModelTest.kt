package co.vitasmarta.mobile.feature.auth

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.feature.auth.domain.AuthRepository
import co.vitasmarta.mobile.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun validatesBeforeSubmittingAndPreventsDoubleTaps() = runTest {
        var calls = 0
        val repository = object : AuthRepository {
            override val session = MutableStateFlow<SessionState>(SessionState.Anonymous)
            override suspend fun login(email: String, password: String): AppResult<Unit> {
                calls++
                delay(10)
                return AppResult.Success(Unit)
            }
            override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
        }
        val viewModel = AuthViewModel(repository)
        viewModel.login()
        assertTrue(viewModel.state.value.emailError)
        assertEquals(0, calls)
        viewModel.emailChanged("test@example.invalid")
        viewModel.passwordChanged("test-password")
        viewModel.login()
        viewModel.login()
        advanceUntilIdle()
        assertEquals(1, calls)
        assertEquals("", viewModel.state.value.password)
    }
}
