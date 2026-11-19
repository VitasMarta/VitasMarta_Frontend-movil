package co.vitasmarta.mobile.feature.catalog

import androidx.lifecycle.SavedStateHandle
import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.catalog.domain.CatalogContentRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogDetail
import co.vitasmarta.mobile.feature.catalog.domain.CatalogItem
import co.vitasmarta.mobile.feature.catalog.domain.CatalogOrder
import co.vitasmarta.mobile.feature.catalog.domain.CatalogPage
import co.vitasmarta.mobile.feature.catalog.domain.CatalogRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogSearchCriteria
import co.vitasmarta.mobile.feature.catalog.domain.CatalogSlot
import co.vitasmarta.mobile.feature.catalog.domain.Category
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogDetailViewModel
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogSearchViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogContentViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val categories = object : CatalogRepository {
        override suspend fun categories(): AppResult<List<Category>> =
            AppResult.Success(emptyList())
    }

    private class Repository : CatalogContentRepository {
        val calls = mutableListOf<Pair<CatalogSearchCriteria, String?>>()
        var search: suspend (CatalogSearchCriteria, String?) -> AppResult<CatalogPage> = { _, _ ->
            AppResult.Success(page(emptyList()))
        }
        override suspend fun search(
            criteria: CatalogSearchCriteria,
            cursor: String?
        ): AppResult<CatalogPage> {
            calls += criteria to cursor
            return search.invoke(criteria, cursor)
        }
        override suspend fun detail(id: String, language: String): AppResult<CatalogDetail> =
            AppResult.Success(
                CatalogDetail(
                    id, "Place", null, emptyList(), null, emptyList(), emptyList(),
                    null, null, null, emptyList(), false, false, null
                )
            )
        override suspend fun slots(id: String): AppResult<List<CatalogSlot>> =
            AppResult.Failure(AppError(ErrorKind.Offline))
    }

    @Test fun queryDebouncesCancelsPreviousAndDoesNotRestoreLateResults() = runTest {
        val repository = Repository().apply {
            search = { criteria, _ ->
                if (criteria.text == "old") {
                    try {
                        delay(1000)
                    } catch (
                        _: CancellationException
                    ) { /* Late transport response. */ }
                }
                AppResult.Success(page(listOf(criteria.text.ifBlank { "initial" })))
            }
        }
        val viewModel = CatalogSearchViewModel(repository, categories, SavedStateHandle())
        viewModel.open("nature")
        advanceUntilIdle()
        viewModel.queryChanged("old")
        advanceTimeBy(400)
        runCurrent()
        viewModel.queryChanged("new")
        advanceUntilIdle()
        assertEquals("new", viewModel.state.value.page?.items?.single()?.id)
        assertEquals(listOf("nature"), viewModel.state.value.criteria.categories)
        assertEquals(3, repository.calls.size)
    }

    @Test fun paginationKeepsResultsOnFailureDeduplicatesAndStopsRepeatedCursors() = runTest {
        var attempt = 0
        val repository = Repository().apply {
            search = { _, cursor ->
                when {
                    cursor == null -> AppResult.Success(page(listOf("a"), "one"))
                    attempt++ == 0 -> AppResult.Failure(AppError(ErrorKind.Offline))
                    else -> AppResult.Success(page(listOf("a", "b"), "one"))
                }
            }
        }
        val viewModel = CatalogSearchViewModel(repository, categories, SavedStateHandle())
        viewModel.open(null)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a"), viewModel.state.value.page?.items?.map { it.id })
        assertEquals(ErrorKind.Offline, viewModel.state.value.moreError?.kind)
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), viewModel.state.value.page?.items?.map { it.id })
        assertEquals(null, viewModel.state.value.page?.nextCursor)
        assertEquals(ErrorKind.InvalidResponse, viewModel.state.value.moreError?.kind)
        viewModel.loadMore()
        assertEquals(3, repository.calls.size)
    }

    @Test fun filtersValidateBeforeNetworkAndPreserveSearchAcrossRestoration() = runTest {
        val repository = Repository()
        val savedState = SavedStateHandle()
        val viewModel = CatalogSearchViewModel(repository, categories, savedState)
        viewModel.open("nature")
        advanceUntilIdle()
        assertFalse(viewModel.applyFilters(listOf("nature"), CatalogOrder.Price, true, "20", "10"))
        assertEquals(1, repository.calls.size)
        viewModel.queryChanged("water")
        assertTrue(
            viewModel.applyFilters(listOf("nature", "sea"), CatalogOrder.Price, true, "10.20", "20")
        )
        advanceUntilIdle()
        val restored = CatalogSearchViewModel(repository, categories, savedState)
        restored.open("other")
        advanceUntilIdle()
        assertEquals("water", restored.state.value.query)
        assertEquals(listOf("nature", "sea"), restored.state.value.criteria.categories)
        assertEquals("10.20", restored.state.value.criteria.minimumPrice?.toPlainString())
        assertEquals(null, repository.calls.last().second)
    }

    @Test fun slotFailureRetainsTheDetailAndDisplaysAPartialFailure() = runTest {
        val viewModel = CatalogDetailViewModel(Repository())
        viewModel.open("p", "es-CO")
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.detail)
        assertFalse(viewModel.state.value.loading)
        assertEquals(ErrorKind.Offline, viewModel.state.value.slotsError?.kind)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test fun invalidMonetaryInputsNeverStartARequestOrEnterSavedState() = runTest {
        val repository = Repository()
        val savedState = SavedStateHandle()
        val viewModel = CatalogSearchViewModel(repository, categories, savedState)
        viewModel.open(null)
        advanceUntilIdle()
        val calls = repository.calls.size
        for (price in listOf("1e100000000", "9".repeat(10_000), "0.123456789")) {
            assertFalse(
                viewModel.applyFilters(emptyList(), CatalogOrder.Price, false, price, "")
            )
            assertEquals(calls, repository.calls.size)
            assertEquals(null, savedState.get<String>("catalog_min"))
        }
        assertTrue(
            viewModel.applyFilters(emptyList(), CatalogOrder.Price, false, "12.3400", "100")
        )
        advanceUntilIdle()
        assertEquals("12.3400", savedState.get<String>("catalog_min"))
        assertEquals("12.3400", repository.calls.last().first.minimumPrice?.toPlainString())
    }

    companion object {
        private fun page(ids: List<String>, cursor: String? = null) = CatalogPage(
            ids.map { CatalogItem(it, mapOf("es" to it), emptyList()) },
            5,
            cursor,
            false,
            null
        )
    }
}
