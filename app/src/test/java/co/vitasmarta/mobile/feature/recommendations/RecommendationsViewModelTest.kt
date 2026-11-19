package co.vitasmarta.mobile.feature.recommendations

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.testToken
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationFeed
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationsRepository
import co.vitasmarta.mobile.feature.recommendations.presentation.RecommendationsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun sessions() = SessionStore().also {
        check(it.install(TokenPair(testToken(), "refresh"), it.snapshot().generation))
    }
    private fun feed(degraded: Boolean = false) = RecommendationFeed(
        "ESTANDAR",
        degraded,
        false,
        "Transparent selection",
        false,
        false,
        emptyList(),
        emptyList()
    )

    @Test fun degradedAndEmptyResponsesRemainUsableContent() = runTest {
        val repository = object : RecommendationsRepository {
            override suspend fun recommendations(): AppResult<RecommendationFeed> =
                AppResult.Success(feed(degraded = true))
        }
        val viewModel = RecommendationsViewModel(repository, sessions())
        runCurrent()
        assertFalse(viewModel.state.value.loading)
        assertTrue(viewModel.state.value.feed?.degraded == true)
        assertTrue(viewModel.state.value.feed?.items?.isEmpty() == true)
        assertNull(viewModel.state.value.error)
    }

    @Test fun aRealFailureCanBeRetriedWithoutKeepingStaleSuggestions() = runTest {
        var calls = 0
        val repository = object : RecommendationsRepository {
            override suspend fun recommendations(): AppResult<RecommendationFeed> {
                calls++
                return if (calls == 1) {
                    AppResult.Failure(AppError(ErrorKind.Offline))
                } else {
                    AppResult.Success(feed())
                }
            }
        }
        val viewModel = RecommendationsViewModel(repository, sessions())
        runCurrent()
        assertEquals(ErrorKind.Offline, viewModel.state.value.error?.kind)
        assertNull(viewModel.state.value.feed)
        viewModel.reload()
        assertTrue(viewModel.state.value.loading)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
        assertEquals(2, calls)
    }

    @Test fun logoutNeverRestoresLatePersonalizedContent() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = object : RecommendationsRepository {
            override suspend fun recommendations(): AppResult<RecommendationFeed> {
                withContext(NonCancellable) { gate.await() }
                return AppResult.Success(feed())
            }
        }
        val sessions = sessions()
        val viewModel = RecommendationsViewModel(repository, sessions)
        runCurrent()
        sessions.clear()
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.state.value.feed)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
    }

    @Test fun anonymousStateDoesNotLoadAndSameUserNewGenerationReloads() = runTest {
        var calls = 0
        val repository = object : RecommendationsRepository {
            override suspend fun recommendations(): AppResult<RecommendationFeed> {
                calls++
                return AppResult.Success(feed())
            }
        }
        val sessions = SessionStore()
        val viewModel = RecommendationsViewModel(repository, sessions)
        runCurrent()
        assertEquals(0, calls)
        check(sessions.install(TokenPair(testToken(), "first"), sessions.snapshot().generation))
        runCurrent()
        assertEquals(1, calls)
        sessions.clear()
        check(sessions.install(TokenPair(testToken(), "second"), sessions.snapshot().generation))
        runCurrent()
        assertEquals(2, calls)
        assertTrue(viewModel.state.value.feed != null)
    }
}
