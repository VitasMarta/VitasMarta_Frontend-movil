package co.vitasmarta.mobile.feature.catalog

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.catalog.domain.CatalogRepository
import co.vitasmarta.mobile.feature.catalog.domain.Category
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogState
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun recoversFromOfflineWithoutKeepingAStaleError() = runTest {
        var attempts = 0
        val repository = object : CatalogRepository {
            override suspend fun categories(): AppResult<List<Category>> = if (attempts++ ==
                0
            ) {
                AppResult.Failure(AppError(ErrorKind.Offline))
            } else {
                AppResult.Success(emptyList())
            }
        }
        val viewModel = CatalogViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is CatalogState.Error)
        viewModel.reload()
        advanceUntilIdle()
        assertTrue(viewModel.state.value is CatalogState.Content)
    }
}
