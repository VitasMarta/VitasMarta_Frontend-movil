package co.vitasmarta.mobile.feature.settings

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.preferences.AppearanceRepository
import co.vitasmarta.mobile.core.preferences.ThemeMode
import co.vitasmarta.mobile.feature.settings.presentation.AppearanceViewModel
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun explicitSelectionSurvivesSystemChangesAndNewViewModel() = runTest {
        val saved = MutableStateFlow(ThemeMode.System)
        val repository = object : AppearanceRepository {
            override val mode = saved
            override suspend fun setMode(mode: ThemeMode) {
                saved.value = mode
            }
        }
        val viewModel = AppearanceViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.ready)
        viewModel.select(ThemeMode.Dark)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.mode.isDark(systemDark = false))
        val restored = AppearanceViewModel(repository)
        advanceUntilIdle()
        assertEquals(ThemeMode.Dark, restored.state.value.mode)
        viewModel.select(ThemeMode.System)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.mode.isDark(systemDark = false))
        assertTrue(viewModel.state.value.mode.isDark(systemDark = true))
    }

    @Test
    fun storageFailureDoesNotBlockTheAppOrPreventAnExplicitTheme() = runTest {
        val repository = object : AppearanceRepository {
            override val mode: Flow<ThemeMode> = flow { throw IOException("unavailable") }
            override suspend fun setMode(mode: ThemeMode): Unit = throw IOException("unavailable")
        }
        val viewModel = AppearanceViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.ready)
        assertTrue(viewModel.state.value.storageError)
        viewModel.select(ThemeMode.Dark)
        advanceUntilIdle()
        assertEquals(ThemeMode.Dark, viewModel.state.value.mode)
        assertTrue(viewModel.state.value.storageError)
    }

    @Test
    fun corruptOrAbsentPreferencesFallBackToSystem() {
        assertEquals(ThemeMode.System, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.System, ThemeMode.fromStored("unknown"))
        assertFalse(ThemeMode.Light.isDark(systemDark = true))
        assertTrue(ThemeMode.Dark.isDark(systemDark = false))
    }
}
