package co.vitasmarta.mobile.feature.booking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.booking.domain.Booking
import co.vitasmarta.mobile.feature.booking.domain.BookingCommand
import co.vitasmarta.mobile.feature.booking.domain.BookingGroup
import co.vitasmarta.mobile.feature.booking.domain.BookingOffer
import co.vitasmarta.mobile.feature.booking.domain.BookingPage
import co.vitasmarta.mobile.feature.booking.domain.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookingListState(
    val group: BookingGroup = BookingGroup.ACTIVAS,
    val page: BookingPage? = null,
    val busy: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class BookingListViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val sessions: SessionStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(BookingListState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var observedSession: SessionState? = null

    init {
        viewModelScope.launch {
            sessions.state.collect { session ->
                job?.cancel()
                if (session is SessionState.Anonymous ||
                    (observedSession != null && observedSession != session)
                ) {
                    repository.clearLocalState()
                }
                observedSession = session
                mutableState.value = BookingListState()
                if (session is SessionState.Authenticated) reload()
            }
        }
    }

    fun groupChanged(group: BookingGroup) {
        if (group == mutableState.value.group) return
        job?.cancel()
        mutableState.value = BookingListState(group)
        load(0)
    }

    fun reload() = load(mutableState.value.page?.page ?: 0)
    fun next() {
        val page = mutableState.value.page ?: return
        if (page.hasNext) load(page.page + 1)
    }
    fun previous() {
        val page = mutableState.value.page ?: return
        if (page.page > 0) load(page.page - 1)
    }

    private fun load(page: Int) {
        if (mutableState.value.busy) return
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null) return
        val group = mutableState.value.group
        mutableState.value = BookingListState(group, busy = true)
        job = viewModelScope.launch {
            val result = repository.list(group, page)
            if (sessions.snapshot().generation != snapshot.generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> BookingListState(group, result.value)
                is AppResult.Failure -> BookingListState(group, error = result.error)
            }
        }
    }
}

data class BookingOffersState(
    val offers: List<BookingOffer> = emptyList(),
    val modifying: Boolean = false,
    val selected: BookingOffer? = null,
    val quantity: String = "1",
    val accepted: Boolean = false,
    val busy: Boolean = false,
    val sending: Boolean = false,
    val locked: Boolean = false,
    val invalid: Boolean = false,
    val created: Booking? = null,
    val retrySeconds: Long = 0,
    val error: AppError? = null
)

@HiltViewModel
class BookingOffersViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val sessions: SessionStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(BookingOffersState())
    val state = mutableState.asStateFlow()
    private var serviceRef = ""
    private var existingReference: String? = null
    private var intent: String? = null
    private var job: Job? = null
    private var observedSession: SessionState? = null
    private var cooldown: Job? = null

    init {
        viewModelScope.launch {
            sessions.state.collect {
                job?.cancel()
                cooldown?.cancel()
                if (it is SessionState.Anonymous ||
                    (observedSession != null && observedSession != it)
                ) {
                    repository.clearLocalState()
                    existingReference = null
                }
                observedSession = it
                intent = null
                mutableState.value = BookingOffersState()
                if (it is SessionState.Authenticated &&
                    serviceRef.isNotBlank()
                ) {
                    load(serviceRef, existingReference)
                }
            }
        }
    }

    fun load(service: String, original: String? = null) {
        if (mutableState.value.busy || mutableState.value.locked) return
        serviceRef = service
        existingReference = original
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null) return
        mutableState.value = BookingOffersState(busy = true, modifying = original != null)
        job = viewModelScope.launch {
            val result = repository.offers(service)
            if (sessions.snapshot().generation != snapshot.generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> BookingOffersState(
                    offers = result.value,
                    modifying =
                    original != null
                )
                is AppResult.Failure -> BookingOffersState(
                    error = result.error,
                    modifying =
                    original != null
                )
            }
        }
    }

    fun select(slotRef: String) {
        val current = mutableState.value
        if (current.locked || current.sending) return
        val offer = current.offers.firstOrNull { it.slotRef == slotRef } ?: return
        mutableState.value = current.copy(selected = offer, accepted = false, invalid = false)
    }
    fun quantityChanged(value: String) {
        val current = mutableState.value
        if (!current.locked) mutableState.value = current.copy(quantity = value, invalid = false)
    }
    fun acceptedChanged(value: Boolean) {
        val current = mutableState.value
        if (!current.locked) mutableState.value = current.copy(accepted = value, invalid = false)
    }

    fun resetRejected() {
        val current = mutableState.value
        if (current.sending ||
            current.retrySeconds > 0 ||
            current.error?.kind !in definitiveRejections
        ) {
            return
        }
        intent = null
        mutableState.value = current.copy(locked = false, error = null, invalid = false)
        load(serviceRef, existingReference)
    }

    fun reserve() {
        val current = mutableState.value
        if (current.sending || current.created != null || current.retrySeconds > 0) return
        val offer = current.selected
        val quantity = current.quantity.toIntOrNull()
        if (offer == null ||
            quantity == null ||
            quantity <= 0 ||
            !current.accepted ||
            (
                !current.locked &&
                    (
                        !offer.reservable ||
                            offer.startsAt <= Instant.now() ||
                            quantity > offer.capacity ||
                            (offer.available != null && quantity > offer.available)
                        )
                )
        ) {
            mutableState.value = current.copy(invalid = true)
            return
        }
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null) return
        val operation = intent ?: UUID.randomUUID().toString().also { intent = it }
        mutableState.value =
            current.copy(sending = true, locked = true, error = null, invalid = false)
        job = viewModelScope.launch {
            val command = BookingCommand(
                offer.serviceRef,
                offer.slotRef,
                quantity,
                offer.policy.reference,
                offer.policy.version,
                true
            )
            val original = existingReference
            val result = if (original == null) {
                repository.create(command, operation)
            } else {
                repository.modify(original, command, operation)
            }
            if (sessions.snapshot().generation != snapshot.generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(
                    sending = false,
                    created = result.value
                )
                is AppResult.Failure -> mutableState.value.copy(
                    sending = false,
                    error = result.error,
                    retrySeconds = result.error.retryAfterSeconds?.coerceAtLeast(0) ?: 0
                )
            }
            startCooldown()
        }
    }

    private fun startCooldown() {
        cooldown?.cancel()
        cooldown = viewModelScope.launch {
            while (mutableState.value.retrySeconds > 0) {
                delay(1000)
                mutableState.value = mutableState.value.copy(
                    retrySeconds = (mutableState.value.retrySeconds - 1).coerceAtLeast(0)
                )
            }
        }
    }
}

data class BookingDetailState(
    val booking: Booking? = null,
    val document: ByteArray? = null,
    val downloading: Boolean = false,
    val busy: Boolean = false,
    val sending: Boolean = false,
    val reason: String = "",
    val invalidReason: Boolean = false,
    val locked: Boolean = false,
    val retrySeconds: Long = 0,
    val error: AppError? = null
)

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val sessions: SessionStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(BookingDetailState())
    val state = mutableState.asStateFlow()
    private var reference = ""
    private var downloadJob: Job? = null
    private var intent: String? = null
    private var job: Job? = null
    private var observedSession: SessionState? = null
    private var cooldown: Job? = null

    init {
        viewModelScope.launch {
            sessions.state.collect {
                job?.cancel()
                cooldown?.cancel()
                if (it is SessionState.Anonymous ||
                    (observedSession != null && observedSession != it)
                ) {
                    repository.clearLocalState()
                    reference = ""
                }
                observedSession = it
                intent = null
                downloadJob?.cancel()
                mutableState.value = BookingDetailState()
                if (it is SessionState.Anonymous) reference = ""
                if (it is SessionState.Authenticated && reference.isNotBlank()) load(reference)
            }
        }
    }

    fun load(value: String) {
        val current = mutableState.value
        if (current.busy || current.sending) return
        if (reference != value) {
            downloadJob?.cancel()
            mutableState.value = BookingDetailState()
        }
        reference = value
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null) return
        // Preserve an unresolved cancellation intent while reconciling with the server.
        mutableState.value = current.copy(busy = true, error = null)
        job = viewModelScope.launch {
            val result = repository.detail(value)
            if (sessions.snapshot().generation != snapshot.generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(
                    booking = result.value,
                    busy = false
                )
                is AppResult.Failure -> mutableState.value.copy(error = result.error, busy = false)
            }
        }
    }

    fun documentHandled() {
        mutableState.value = mutableState.value.copy(document = null)
    }

    fun download() {
        if (mutableState.value.downloading || mutableState.value.document != null) return
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null || reference.isBlank()) return
        mutableState.value = mutableState.value.copy(downloading = true, error = null)
        downloadJob = viewModelScope.launch {
            val result = repository.receipt(reference)
            if (sessions.snapshot().generation != snapshot.generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(
                    document = result.value,
                    downloading = false
                )
                is AppResult.Failure -> mutableState.value.copy(
                    downloading = false,
                    error = result.error
                )
            }
        }
    }

    fun reasonChanged(value: String) {
        if (!mutableState.value.locked) {
            mutableState.value = mutableState.value.copy(reason = value, invalidReason = false)
        }
    }

    fun resetRejected() {
        if (mutableState.value.sending ||
            mutableState.value.retrySeconds > 0 ||
            mutableState.value.error?.kind !in definitiveRejections
        ) {
            return
        }
        intent = null
        mutableState.value = mutableState.value.copy(locked = false, error = null)
    }

    fun cancel() {
        val current = mutableState.value
        if (current.sending || current.retrySeconds > 0) return
        val booking = current.booking ?: return
        if (!current.locked && !booking.canCancel(Instant.now())) {
            mutableState.value = current.copy(error = AppError(ErrorKind.Conflict))
            return
        }
        val reason = current.reason.trim()
        if (reason.isBlank() || reason.length > 2000) {
            mutableState.value = current.copy(invalidReason = true)
            return
        }
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null) return
        val operation = intent ?: UUID.randomUUID().toString().also { intent = it }
        mutableState.value = current.copy(sending = true, locked = true, error = null)
        job = viewModelScope.launch {
            val result = repository.cancel(reference, reason, operation)
            if (sessions.snapshot().generation != snapshot.generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> BookingDetailState(booking = result.value)
                is AppResult.Failure -> mutableState.value.copy(
                    sending = false,
                    error = result.error,
                    retrySeconds = result.error.retryAfterSeconds?.coerceAtLeast(0) ?: 0
                )
            }
            cooldown?.cancel()
            cooldown = viewModelScope.launch {
                while (mutableState.value.retrySeconds > 0) {
                    delay(1000)
                    mutableState.value = mutableState.value.copy(
                        retrySeconds = (mutableState.value.retrySeconds - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }
}

internal val definitiveRejections = setOf(
    ErrorKind.Validation,
    ErrorKind.Conflict,
    ErrorKind.Forbidden,
    ErrorKind.NotFound,
    ErrorKind.Locked,
    ErrorKind.RateLimited
)
