package co.vitasmarta.mobile.feature.auth

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.feature.auth.domain.AccountRecoveryRepository
import co.vitasmarta.mobile.feature.auth.presentation.RecoveryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun validatesAndDoesNotSendDuplicateRecoveryEmails() = runTest {
        var calls = 0
        val repository = object : AccountRecoveryRepository {
            override suspend fun recover(email: String): AppResult<Unit> {
                calls++
                delay(10)
                return AppResult.Success(Unit)
            }
        }
        val viewModel = RecoveryViewModel(repository)
        viewModel.recover()
        assertTrue(viewModel.state.value.invalidEmail)
        assertEquals(0, calls)
        viewModel.emailChanged("test@example.invalid")
        viewModel.recover()
        viewModel.recover()
        advanceUntilIdle()
        assertEquals(1, calls)
        assertTrue(viewModel.state.value.accepted)
        assertEquals("", viewModel.state.value.email)
    }
}
