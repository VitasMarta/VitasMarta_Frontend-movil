package co.vitasmarta.mobile.feature.booking

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.testToken
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.booking.domain.Booking
import co.vitasmarta.mobile.feature.booking.domain.BookingCommand
import co.vitasmarta.mobile.feature.booking.domain.BookingGroup
import co.vitasmarta.mobile.feature.booking.domain.BookingOffer
import co.vitasmarta.mobile.feature.booking.domain.BookingPage
import co.vitasmarta.mobile.feature.booking.domain.BookingRepository
import co.vitasmarta.mobile.feature.booking.domain.BookingStatus
import co.vitasmarta.mobile.feature.booking.presentation.BookingDetailViewModel
import co.vitasmarta.mobile.feature.booking.presentation.BookingListViewModel
import co.vitasmarta.mobile.feature.booking.presentation.BookingOffersViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun offersRequireSlotQuantityAndExplicitPolicyAndFreezeTheSubmittedIntent() = runTest {
        val sessions = bookingSessions()
        val repository = RecordingBookingRepository()
        val viewModel = BookingOffersViewModel(repository, sessions)
        runCurrent()
        viewModel.load("service-1")
        advanceUntilIdle()
        viewModel.reserve()
        assertTrue(viewModel.state.value.invalid)
        assertTrue(repository.creations.isEmpty())
        viewModel.select("slot-1")
        viewModel.acceptedChanged(true)
        viewModel.quantityChanged("99")
        viewModel.reserve()
        assertTrue(repository.creations.isEmpty())
        viewModel.quantityChanged("2")
        repository.gate = CompletableDeferred()
        repository.mutation = AppResult.Failure(AppError(ErrorKind.Timeout))
        viewModel.reserve()
        viewModel.reserve()
        viewModel.quantityChanged("3")
        runCurrent()
        assertEquals(1, repository.creations.size)
        assertEquals(2, repository.creations.single().first.quantity)
        assertEquals("2", viewModel.state.value.quantity)
        repository.gate?.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.locked)
        repository.mutation = AppResult.Success(testBooking)
        viewModel.reserve()
        advanceUntilIdle()
        assertEquals(2, repository.creations.size)
        assertEquals(repository.creations.first(), repository.creations.last())
        assertEquals(BookingStatus.PENDIENTE_PAGO_EXTERNO, viewModel.state.value.created?.status)
    }

    @Test fun unknownAvailabilityStillHonorsTheKnownTotalCapacity() = runTest {
        val repository = RecordingBookingRepository().apply {
            offered = bookingOffer.copy(available = null, capacity = 3)
        }
        val viewModel = BookingOffersViewModel(repository, bookingSessions())
        runCurrent()
        viewModel.load("service-1")
        advanceUntilIdle()
        assertNull(viewModel.state.value.offers.single().available)
        viewModel.select("slot-1")
        viewModel.acceptedChanged(true)
        viewModel.quantityChanged("4")
        viewModel.reserve()
        assertTrue(viewModel.state.value.invalid)
        assertTrue(repository.creations.isEmpty())
        viewModel.quantityChanged("2")
        viewModel.reserve()
        advanceUntilIdle()
        assertEquals(1, repository.creations.size)
    }

    @Test fun definitiveConflictRequiresExplicitRefreshBeforeChangingOffer() = runTest {
        val sessions = bookingSessions()
        val repository = RecordingBookingRepository()
        val viewModel = BookingOffersViewModel(repository, sessions)
        runCurrent()
        viewModel.load("service-1")
        advanceUntilIdle()
        viewModel.select("slot-1")
        viewModel.acceptedChanged(true)
        repository.mutation = AppResult.Failure(AppError(ErrorKind.Conflict))
        viewModel.reserve()
        advanceUntilIdle()
        viewModel.quantityChanged("4")
        assertEquals("1", viewModel.state.value.quantity)
        viewModel.resetRejected()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.locked)
        assertFalse(viewModel.state.value.accepted)
        assertNull(viewModel.state.value.selected)
        viewModel.select("slot-1")
        viewModel.acceptedChanged(true)
        repository.mutation = AppResult.Success(testBooking)
        viewModel.reserve()
        advanceUntilIdle()
        assertNotEquals(repository.creations.first().second, repository.creations.last().second)
    }

    @Test fun rateLimitCannotBeBypassedByResettingTheRejectedSelection() = runTest {
        val repository = RecordingBookingRepository()
        val viewModel = BookingOffersViewModel(repository, bookingSessions())
        runCurrent()
        viewModel.load("service-1")
        advanceUntilIdle()
        viewModel.select("slot-1")
        viewModel.acceptedChanged(true)
        repository.mutation =
            AppResult.Failure(AppError(ErrorKind.RateLimited, retryAfterSeconds = 2))
        viewModel.reserve()
        runCurrent()
        viewModel.resetRejected()
        viewModel.reserve()
        assertTrue(viewModel.state.value.locked)
        assertEquals(2L, viewModel.state.value.retrySeconds)
        assertEquals(1, repository.creations.size)
        advanceTimeBy(2000)
        runCurrent()
        viewModel.resetRejected()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.locked)
    }

    @Test fun modificationKeepsTheOriginalReferenceAndUsesTheSameValidatedOfferFlow() = runTest {
        val repository = RecordingBookingRepository().apply {
            mutation =
                AppResult.Success(
                    testBooking.copy(reference = "replacement", originalRef = "original")
                )
        }
        val viewModel = BookingOffersViewModel(repository, bookingSessions())
        runCurrent()
        viewModel.load("service-1", "original")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.modifying)
        viewModel.select("slot-1")
        viewModel.acceptedChanged(true)
        viewModel.reserve()
        advanceUntilIdle()
        assertTrue(repository.creations.isEmpty())
        assertEquals("original", repository.modifications.single().first)
        assertEquals("replacement", viewModel.state.value.created?.reference)
    }

    @Test fun cancellationRetainsIntentAndRejectsDuplicates() = runTest {
        val repository = RecordingBookingRepository()
        val viewModel = BookingDetailViewModel(repository, bookingSessions())
        runCurrent()
        viewModel.load("booking-1")
        advanceUntilIdle()
        viewModel.cancel()
        assertTrue(viewModel.state.value.invalidReason)
        assertTrue(repository.cancellations.isEmpty())
        viewModel.reasonChanged("Changed plans")
        repository.gate = CompletableDeferred()
        repository.mutation = AppResult.Failure(AppError(ErrorKind.Timeout))
        viewModel.cancel()
        viewModel.cancel()
        runCurrent()
        assertEquals(1, repository.cancellations.size)
        repository.gate?.complete(Unit)
        advanceUntilIdle()
        viewModel.reasonChanged("A different reason")
        assertEquals("Changed plans", viewModel.state.value.reason)
        repository.mutation = AppResult.Success(testBooking.copy(status = BookingStatus.CANCELADA))
        viewModel.cancel()
        advanceUntilIdle()
        assertEquals(repository.cancellations.first(), repository.cancellations.last())
        assertEquals(BookingStatus.CANCELADA, viewModel.state.value.booking?.status)
        assertFalse(viewModel.state.value.locked)
    }

    @Test fun logoutClearsHistoryOffersAndDownloadedDocumentsIncludingSameUserRelogin() = runTest {
        val repository = RecordingBookingRepository()
        val sessions = bookingSessions()
        val list = BookingListViewModel(repository, sessions)
        val offers = BookingOffersViewModel(repository, sessions)
        val detail = BookingDetailViewModel(repository, sessions)
        runCurrent()
        offers.load("service-1")
        detail.load("booking-1")
        advanceUntilIdle()
        detail.download()
        advanceUntilIdle()
        assertTrue(list.state.value.page?.items?.isNotEmpty() == true)
        assertTrue(offers.state.value.offers.isNotEmpty())
        assertTrue(detail.state.value.document != null)
        // No intermediate collection: generation on Authenticated must reveal a new session.
        val generation = sessions.clear()
        repository.listResult = AppResult.Failure(AppError(ErrorKind.Forbidden))
        assertTrue(sessions.install(TokenPair(testToken(), "new-refresh"), generation))
        runCurrent()
        assertNull(list.state.value.page)
        assertNull(detail.state.value.document)
        assertNull(offers.state.value.created)
        sessions.clear()
        runCurrent()
        assertNull(list.state.value.page)
        assertTrue(offers.state.value.offers.isEmpty())
        assertNull(detail.state.value.booking)
        assertNull(detail.state.value.document)
        assertTrue(repository.clears > 0)
    }

    @Test fun documentIsHandedOffOnlyOnceAndNeverAutomaticallyDownloaded() = runTest {
        val repository = RecordingBookingRepository()
        val viewModel = BookingDetailViewModel(repository, bookingSessions())
        runCurrent()
        viewModel.load("booking-1")
        advanceUntilIdle()
        assertEquals(0, repository.downloads)
        viewModel.download()
        viewModel.download()
        advanceUntilIdle()
        assertEquals(1, repository.downloads)
        assertTrue(viewModel.state.value.document?.isNotEmpty() == true)
        viewModel.documentHandled()
        assertNull(viewModel.state.value.document)
    }
}

private class RecordingBookingRepository : BookingRepository {
    var clears = 0
    var downloads = 0
    var offered = bookingOffer
    var gate: CompletableDeferred<Unit>? = null
    var mutation: AppResult<Booking> = AppResult.Success(testBooking)
    var listResult: AppResult<BookingPage> = AppResult.Success(
        BookingPage(listOf(testBooking), 1, 0, 20)
    )
    val creations = mutableListOf<Pair<BookingCommand, String>>()
    val modifications = mutableListOf<Triple<String, BookingCommand, String>>()
    val cancellations = mutableListOf<Triple<String, String, String>>()
    override fun clearLocalState() {
        clears++
    }
    override suspend fun offers(serviceRef: String): AppResult<List<BookingOffer>> =
        AppResult.Success(listOf(offered))
    override suspend fun list(group: BookingGroup, page: Int): AppResult<BookingPage> = listResult
    override suspend fun detail(reference: String): AppResult<Booking> =
        AppResult.Success(testBooking)
    override suspend fun receipt(reference: String): AppResult<ByteArray> {
        downloads++
        return AppResult.Success("%PDF-1.7 test".toByteArray())
    }
    override suspend fun create(command: BookingCommand, intent: String): AppResult<Booking> {
        creations += command to intent
        gate?.await()
        return mutation
    }
    override suspend fun modify(
        reference: String,
        command: BookingCommand,
        intent: String
    ): AppResult<Booking> {
        modifications += Triple(reference, command, intent)
        gate?.await()
        return mutation
    }
    override suspend fun cancel(
        reference: String,
        reason: String,
        intent: String
    ): AppResult<Booking> {
        cancellations += Triple(reference, reason, intent)
        gate?.await()
        return mutation
    }
}
