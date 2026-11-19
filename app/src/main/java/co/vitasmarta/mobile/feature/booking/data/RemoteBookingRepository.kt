package co.vitasmarta.mobile.feature.booking.data

import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.AuthenticatedApiExecutor
import co.vitasmarta.mobile.feature.booking.domain.Booking
import co.vitasmarta.mobile.feature.booking.domain.BookingCommand
import co.vitasmarta.mobile.feature.booking.domain.BookingGroup
import co.vitasmarta.mobile.feature.booking.domain.BookingOffer
import co.vitasmarta.mobile.feature.booking.domain.BookingPage
import co.vitasmarta.mobile.feature.booking.domain.BookingRepository
import co.vitasmarta.mobile.feature.booking.domain.BookingStatus
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.DateTimeException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteBookingRepository(
    private val api: BookingApi?,
    private val authenticated: AuthenticatedApiExecutor,
    private val sessions: SessionStore
) : BookingRepository {
    private class Mutation(
        val scope: String,
        val payload: List<String>,
        val generation: Long,
        val correlation: String = UUID.randomUUID().toString(),
        var key: String? = null,
        var receipt: Booking? = null
    )
    private val mutations = ConcurrentHashMap<String, Mutation>()
    private val mutationLock = Mutex()

    override fun clearLocalState() {
        mutations.clear()
    }

    override suspend fun offers(serviceRef: String): AppResult<List<BookingOffer>> {
        val remote = api ?: return failure(ErrorKind.Configuration)
        if (serviceRef.isBlank()) return failure(ErrorKind.Validation)
        return mapped(
            authenticated.execute { bearer, id ->
                remote.offers(serviceRef, bearer, id)
            }
        ) {
            require(it.serviceRef == serviceRef)
            require(it.ofertas.map(OfferDto::slotRef).distinct().size == it.ofertas.size)
            it.ofertas.map { offer ->
                require(offer.serviceRef == serviceRef)
                offer.domain()
            }
        }
    }

    override suspend fun list(group: BookingGroup, page: Int): AppResult<BookingPage> {
        val remote = api ?: return failure(ErrorKind.Configuration)
        if (page < 0) return failure(ErrorKind.Validation)
        return mapped(
            authenticated.execute { bearer, id ->
                remote.list(group.name, page, 20, bearer, id)
            }
        ) {
            require(it.page == page && it.size > 0 && it.total >= 0)
            require(it.content.size <= it.size)
            require(it.content.map(BookingDto::reference).distinct().size == it.content.size)
            BookingPage(it.content.map(BookingDto::domain), it.total, it.page, it.size)
        }
    }

    override suspend fun detail(reference: String): AppResult<Booking> {
        val remote = api ?: return failure(ErrorKind.Configuration)
        if (reference.isBlank()) return failure(ErrorKind.Validation)
        return mapped(
            authenticated.execute { bearer, id ->
                remote.detail(reference, bearer, id)
            }
        ) {
            require(it.reference == reference)
            it.domain()
        }
    }

    override suspend fun create(command: BookingCommand, intent: String): AppResult<Booking> =
        createOrModify(command, intent, null)

    override suspend fun modify(
        reference: String,
        command: BookingCommand,
        intent: String
    ): AppResult<Booking> {
        if (reference.isBlank()) return failure(ErrorKind.Validation)
        return createOrModify(command, intent, reference)
    }

    private suspend fun createOrModify(
        command: BookingCommand,
        intent: String,
        original: String?
    ): AppResult<Booking> {
        if (command.serviceRef.isBlank() ||
            command.slotRef.isBlank() ||
            command.quantity <= 0 ||
            command.policyRef.isBlank() ||
            command.policyVersion.isBlank() ||
            !command.accepted
        ) {
            return failure(ErrorKind.Validation)
        }
        val request = CreateBookingDto(
            command.serviceRef,
            command.slotRef,
            command.quantity,
            PolicyAcceptanceDto(true, command.policyRef, command.policyVersion)
        )
        return mutate(
            intent,
            if (original == null) "iniciar" else "modificar",
            original,
            listOf(
                command.serviceRef,
                command.slotRef,
                command.quantity.toString(),
                command.policyRef,
                command.policyVersion
            ),
            if (original == null) 201 else 200,
            verify = {
                it.serviceRef == command.serviceRef &&
                    it.slotRef == command.slotRef &&
                    it.quantity == command.quantity &&
                    (original == null || (it.originalRef == original && it.reference != original))
            }
        ) { remote, key, bearer, id ->
            if (original == null) {
                remote.create(request, key, bearer, id)
            } else {
                remote.modify(original, request, key, bearer, id)
            }
        }
    }

    override suspend fun receipt(reference: String): AppResult<ByteArray> {
        val remote = api ?: return failure(ErrorKind.Configuration)
        if (reference.isBlank()) return failure(ErrorKind.Validation)
        val generation = sessions.snapshot().generation
        var status = 0
        var openedBody: okhttp3.ResponseBody? = null
        return try {
            when (
                val result = authenticated.execute { bearer, id ->
                    remote.receipt(reference, bearer, id).also {
                        status = it.code()
                        openedBody = it.body()
                    }
                }
            ) {
                is AppResult.Failure -> result
                is AppResult.Success -> try {
                    val bytes = withContext(Dispatchers.IO) {
                        result.value.use { body ->
                            require(status == 200)
                            require(body.contentType()?.subtype == "pdf")
                            require(body.contentLength() <= MAX_PDF_BYTES)
                            body.byteStream().use { input ->
                                val output = ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    require(output.size() + count <= MAX_PDF_BYTES)
                                    output.write(buffer, 0, count)
                                }
                                output.toByteArray().also {
                                    require(
                                        it.size >= 5 &&
                                            it.copyOfRange(0, 5)
                                                .contentEquals(byteArrayOf(37, 80, 68, 70, 45))
                                    )
                                }
                            }
                        }
                    }
                    if (sessions.snapshot().generation != generation) {
                        failure(ErrorKind.Unauthorized)
                    } else {
                        AppResult.Success(bytes)
                    }
                } catch (_: IOException) {
                    failure(ErrorKind.Offline)
                } catch (_: IllegalArgumentException) {
                    failure(ErrorKind.InvalidResponse)
                }
            }
        } finally {
            // Also close a streaming body discarded by the session generation guard.
            openedBody?.close()
        }
    }

    override suspend fun cancel(
        reference: String,
        reason: String,
        intent: String
    ): AppResult<Booking> {
        val trimmed = reason.trim()
        if (reference.isBlank() || trimmed.isBlank() || trimmed.length > 2000) {
            return failure(ErrorKind.Validation)
        }
        return mutate(
            intent,
            "cancelar",
            reference,
            listOf(reference, trimmed),
            200,
            verify = { it.reference == reference && it.status == BookingStatus.CANCELADA }
        ) { remote, key, bearer, id ->
            remote.cancel(reference, CancelBookingDto(trimmed), key, bearer, id)
        }
    }

    private suspend fun mutate(
        intent: String,
        operation: String,
        reference: String?,
        payload: List<String>,
        successStatus: Int,
        verify: (Booking) -> Boolean,
        call: suspend (BookingApi, String, String, String) -> Response<BookingDto>
    ): AppResult<Booking> = mutationLock.withLock {
        val remote = api ?: return@withLock failure(ErrorKind.Configuration)
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null) return@withLock failure(ErrorKind.Unauthorized)
        if (intent.isBlank()) return@withLock failure(ErrorKind.Validation)
        mutations.entries.removeAll { it.value.generation != snapshot.generation }
        // Keep uncertain intents; completed receipts may be discarded to bound memory.
        if (mutations.size >= 64 && !mutations.containsKey(intent)) {
            val completed = mutations.entries.firstOrNull { it.value.receipt != null }?.key
            if (completed != null) {
                mutations.remove(completed)
            } else {
                return@withLock failure(ErrorKind.Conflict)
            }
        }
        val scope = if (reference == null) operation else "$operation:$reference"
        val record = mutations.getOrPut(intent) { Mutation(scope, payload, snapshot.generation) }
        if (record.scope != scope || record.payload != payload) {
            return@withLock failure(ErrorKind.Validation)
        }
        record.receipt?.let { return@withLock AppResult.Success(it) }
        var code = 0
        if (record.key == null) {
            when (
                val result = authenticated.execute { bearer, _ ->
                    remote.issueKey(KeyRequest(operation, reference), bearer, record.correlation)
                        .also { code = it.code() }
                }
            ) {
                is AppResult.Failure -> return@withLock result
                is AppResult.Success -> {
                    val key = result.value.key
                    if (code != 201 ||
                        result.value.operacion != scope ||
                        key.isBlank() ||
                        key.any(Char::isISOControl)
                    ) {
                        return@withLock failure(ErrorKind.InvalidResponse)
                    }
                    record.key = key
                }
            }
        }
        val key = record.key ?: return@withLock failure(ErrorKind.InvalidResponse)
        val result = mapped(
            authenticated.execute { bearer, _ ->
                call(remote, key, bearer, record.correlation).also { code = it.code() }
            }
        ) {
            require(code == successStatus)
            it.domain().also { booking -> require(verify(booking)) }
        }
        if (sessions.snapshot().generation != snapshot.generation) {
            mutations.clear()
            return@withLock failure(ErrorKind.Unauthorized)
        }
        if (result is AppResult.Success) record.receipt = result.value
        result
    }

    private fun <T, R> mapped(result: AppResult<T>, transform: (T) -> R): AppResult<R> =
        when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> try {
                AppResult.Success(transform(result.value))
            } catch (_: IllegalArgumentException) {
                failure(ErrorKind.InvalidResponse)
            } catch (_: DateTimeException) {
                failure(ErrorKind.InvalidResponse)
            }
        }

    private companion object {
        const val MAX_PDF_BYTES = 10 * 1024 * 1024
    }

    private fun failure(kind: ErrorKind) = AppResult.Failure(AppError(kind))
}
