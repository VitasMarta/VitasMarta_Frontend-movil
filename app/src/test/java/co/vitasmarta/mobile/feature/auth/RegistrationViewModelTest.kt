package co.vitasmarta.mobile.feature.auth

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.auth.domain.RegistrationDetails
import co.vitasmarta.mobile.feature.auth.domain.RegistrationPasswordPolicy
import co.vitasmarta.mobile.feature.auth.domain.RegistrationRepository
import co.vitasmarta.mobile.feature.auth.presentation.RegistrationViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class Repository : RegistrationRepository {
        val requests = mutableListOf<RegistrationDetails>()
        var response: AppResult<Unit> = AppResult.Success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        var cancel = false
        override suspend fun register(details: RegistrationDetails): AppResult<Unit> {
            requests += details
            gate?.await()
            if (cancel) throw CancellationException("Cancelled in test")
            return response
        }
    }

    private fun fill(viewModel: RegistrationViewModel) {
        viewModel.emailChanged(" test@example.invalid ")
        viewModel.passwordChanged("Correcta1234!")
        viewModel.treatmentChanged(true)
    }

    @Test fun validationStartsAfterSubmitAndOptionalConsentsNeverBlockRegistration() = runTest {
        val repository = Repository()
        val viewModel = RegistrationViewModel(repository)
        assertFalse(viewModel.state.value.emailError)
        assertFalse(viewModel.state.value.passwordError)
        assertFalse(viewModel.state.value.treatment)
        assertFalse(viewModel.state.value.commercial)
        assertFalse(viewModel.state.value.profiling)
        viewModel.register()
        assertTrue(viewModel.state.value.emailError)
        assertTrue(viewModel.state.value.passwordError)
        assertTrue(viewModel.state.value.treatmentError)
        assertTrue(repository.requests.isEmpty())
        fill(viewModel)
        assertFalse(viewModel.state.value.emailError)
        assertFalse(viewModel.state.value.passwordError)
        assertFalse(viewModel.state.value.treatmentError)
        viewModel.register()
        advanceUntilIdle()
        assertEquals(1, repository.requests.size)
        assertEquals("test@example.invalid", repository.requests.single().email)
        assertFalse(repository.requests.single().commercial)
        assertFalse(repository.requests.single().profiling)
        assertTrue(viewModel.state.value.created)
        assertEquals("", viewModel.state.value.password)
        assertEquals("", viewModel.state.value.email)
    }

    @Test fun ignoresDuplicateSubmissionsAndEditsWhilePendingOrCompleted() = runTest {
        val repository = Repository().apply { gate = CompletableDeferred() }
        val viewModel = RegistrationViewModel(repository)
        fill(viewModel)
        viewModel.commercialChanged(true)
        viewModel.register()
        viewModel.register()
        viewModel.emailChanged("other@example.invalid")
        viewModel.profilingChanged(true)
        runCurrent()
        assertEquals(1, repository.requests.size)
        assertTrue(repository.requests.single().commercial)
        assertFalse(repository.requests.single().profiling)
        repository.gate?.complete(Unit)
        advanceUntilIdle()
        viewModel.register()
        assertEquals(1, repository.requests.size)
        assertTrue(viewModel.state.value.created)
        assertFalse(viewModel.state.value.busy)
    }

    @Test fun errorsClearThePasswordAndRetryAfterCannotBeBypassedByEditing() = runTest {
        val repository = Repository().apply {
            response = AppResult.Failure(AppError(ErrorKind.RateLimited, retryAfterSeconds = 2))
        }
        val viewModel = RegistrationViewModel(repository)
        fill(viewModel)
        viewModel.profilingChanged(true)
        viewModel.register()
        runCurrent()
        assertEquals("", viewModel.state.value.password)
        assertFalse(viewModel.state.value.created)
        assertTrue(viewModel.state.value.profiling)
        assertEquals(2L, viewModel.state.value.retrySeconds)
        viewModel.passwordChanged("Correcta1234!")
        viewModel.register()
        assertEquals(1, repository.requests.size)
        advanceTimeBy(2000)
        runCurrent()
        repository.response = AppResult.Success(Unit)
        viewModel.register()
        advanceUntilIdle()
        assertEquals(2, repository.requests.size)
        assertTrue(viewModel.state.value.created)
        assertFalse(viewModel.state.value.toString().contains("test@example.invalid"))
        assertTrue(repository.requests.all { !it.toString().contains("Correcta") })
    }

    @Test fun cancellationDoesNotClaimSuccessAndClearsSensitiveInput() = runTest {
        val repository = Repository().apply { cancel = true }
        val viewModel = RegistrationViewModel(repository)
        fill(viewModel)
        viewModel.register()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.busy)
        assertFalse(viewModel.state.value.created)
        assertEquals("", viewModel.state.value.password)
    }

    @Test fun passwordRulesMatchM7IncludingWhitespaceAndUpperLengthBound() {
        assertTrue(RegistrationPasswordPolicy.accepts("Correcta1234!"))
        for (value in listOf(
            "Corta1!",
            "correcta1234!",
            "CORRECTA1234!",
            "Correctaaaaa!",
            "Correcta12345",
            "Correcta1234 ",
            "A1!" + "x".repeat(126)
        )) {
            assertFalse(value, RegistrationPasswordPolicy.accepts(value))
        }
        assertTrue(RegistrationPasswordPolicy.accepts("ÁrbolSeguro1!"))
    }
}
