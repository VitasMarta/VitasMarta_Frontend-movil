package co.vitasmarta.mobile.feature.booking.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.designsystem.ErrorNotice
import co.vitasmarta.mobile.feature.booking.domain.Booking
import co.vitasmarta.mobile.feature.booking.domain.BookingGroup
import co.vitasmarta.mobile.feature.booking.domain.BookingStatus
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

@Composable
fun BookingListRoute(
    onOpen: (String) -> Unit,
    onExplore: () -> Unit,
    viewModel: BookingListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BookingListScreen(
        state,
        viewModel::groupChanged,
        viewModel::reload,
        viewModel::previous,
        viewModel::next,
        onOpen,
        onExplore
    )
}

@Composable
fun BookingListScreen(
    state: BookingListState,
    onGroup: (BookingGroup) -> Unit,
    onReload: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: (String) -> Unit,
    onExplore: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            BookingHeading(R.string.booking_list_title)
            Text(stringResource(R.string.booking_list_description))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BookingGroup.entries.forEach { group ->
                    FilterChip(
                        selected = state.group == group,
                        onClick = { onGroup(group) },
                        label = { Text(stringResource(group.label())) }
                    )
                }
            }
        }
        if (state.busy) item { BookingLoading() }
        state.error?.let { error -> item { ErrorNotice(error, onRetry = onReload) } }
        state.page?.let { page ->
            if (page.items.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(36.dp))
                            Text(
                                stringResource(R.string.booking_empty_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(stringResource(R.string.booking_empty_description))
                            Button(onClick = onExplore) {
                                Text(stringResource(R.string.booking_explore))
                            }
                        }
                    }
                }
            }
            items(page.items, key = { it.reference }) { booking ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BookingStatusLabel(booking.status)
                    Text(
                        bookingDate(booking.startsAt),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.booking_reference, booking.reference),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(bookingMoney(booking.total, booking.currency))
                    OutlinedButton(onClick = { onOpen(booking.reference) }) {
                        Text(stringResource(R.string.booking_open))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onPrevious, enabled = page.page > 0) {
                        Text(stringResource(R.string.booking_previous))
                    }
                    Text(stringResource(R.string.booking_page, page.page + 1))
                    TextButton(onNext, enabled = page.hasNext) {
                        Text(stringResource(R.string.booking_next))
                    }
                }
            }
        }
    }
}

@Composable
fun BookingOffersRoute(
    serviceRef: String,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    existingReference: String? = null,
    viewModel: BookingOffersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(serviceRef, existingReference) { viewModel.load(serviceRef, existingReference) }
    BookingOffersScreen(
        state, onBack, { viewModel.load(serviceRef, existingReference) }, viewModel::select,
        viewModel::quantityChanged, viewModel::acceptedChanged, viewModel::reserve,
        viewModel::resetRejected, onCreated
    )
}

@Composable
fun BookingOffersScreen(
    state: BookingOffersState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onSelect: (String) -> Unit,
    onQuantity: (String) -> Unit,
    onAccepted: (Boolean) -> Unit,
    onReserve: () -> Unit,
    onReset: () -> Unit,
    onCreated: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            TextButton(onBack, enabled = !state.sending) { Text(stringResource(R.string.back)) }
            BookingHeading(
                if (state.modifying) {
                    R.string.booking_modify_title
                } else {
                    R.string.booking_offers_title
                }
            )
            Text(stringResource(R.string.booking_offers_description))
        }
        if (state.busy) item { BookingLoading() }
        val created = state.created
        if (created != null) {
            item {
                Text(
                    stringResource(
                        if (state.modifying) R.string.booking_modified else R.string.booking_created
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                BookingStatusLabel(created.status)
                Text(stringResource(R.string.booking_reference, created.reference))
                Button(onClick = { onCreated(created.reference) }) {
                    Text(stringResource(R.string.booking_open))
                }
            }
        } else {
            if (!state.busy && state.error == null && state.offers.isEmpty()) {
                item {
                    Text(stringResource(R.string.booking_offers_empty))
                }
            }
            items(state.offers, key = { it.slotRef }) { offer ->
                val enabled = !state.locked && offer.reservable && offer.startsAt > Instant.now()
                Surface(
                    color = if (state.selected?.slotRef == offer.slotRef) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        Modifier.fillMaxWidth().selectable(
                            state.selected?.slotRef == offer.slotRef,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(offer.slotRef) }
                        ).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            state.selected?.slotRef == offer.slotRef,
                            null,
                            enabled = enabled
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                bookingDate(offer.startsAt),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(bookingDate(offer.endsAt))
                            Text(bookingMoney(offer.amount, offer.currency))
                            Text(
                                stringResource(
                                    if (offer.unit == "POR_CUPO") {
                                        R.string.booking_per_place
                                    } else {
                                        R.string.booking_per_reservation
                                    }
                                )
                            )
                            Text(
                                if (offer.available == null) {
                                    stringResource(R.string.booking_availability_unknown)
                                } else {
                                    stringResource(R.string.booking_available, offer.available)
                                }
                            )
                            if (!offer.reservable) {
                                Text(stringResource(R.string.booking_not_reservable))
                            }
                        }
                    }
                }
            }
            state.selected?.let { offer ->
                item {
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = onQuantity,
                        enabled = !state.locked,
                        singleLine = true,
                        label = { Text(stringResource(R.string.booking_quantity)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    state.quantity.toIntOrNull()?.takeIf { it > 0 }?.let { quantity ->
                        Text(
                            stringResource(
                                R.string.booking_offer_total,
                                bookingMoney(offer.totalFor(quantity), offer.currency)
                            ),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(stringResource(R.string.booking_offer_recheck))
                    Text(
                        stringResource(R.string.booking_policy_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(offer.policy.content)
                    offer.policy.penaltyPercent?.let {
                        Text(stringResource(R.string.booking_policy_penalty, it))
                    }
                    offer.policy.cancellationHours?.let {
                        Text(pluralStringResource(R.plurals.booking_policy_hours, it, it))
                    }
                    if (offer.needsExternalPayment) {
                        Text(stringResource(R.string.booking_external_payment))
                    }
                    if (offer.needsApproval) Text(stringResource(R.string.booking_needs_approval))
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(
                            state.accepted,
                            enabled = !state.locked,
                            role = Role.Checkbox,
                            onValueChange = onAccepted
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(state.accepted, null, enabled = !state.locked)
                        Text(stringResource(R.string.booking_accept_policy))
                    }
                }
            }
            if (state.invalid) {
                item {
                    Text(
                        stringResource(R.string.booking_invalid),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
            state.error?.let { error ->
                item {
                    ErrorNotice(error, onRetry = if (!state.locked) onReload else null)
                    if (state.locked && error.kind !in definitiveRejections) {
                        Text(stringResource(R.string.booking_uncertain))
                    }
                    if (state.locked && error.kind in definitiveRejections) {
                        OutlinedButton(
                            onClick = onReset,
                            enabled = !state.sending && state.retrySeconds == 0L
                        ) {
                            Text(stringResource(R.string.booking_change_selection))
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onReserve,
                    enabled = !state.busy &&
                        !state.sending &&
                        state.offers.isNotEmpty() &&
                        state.retrySeconds == 0L,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text(
                        when {
                            state.sending -> stringResource(R.string.booking_sending)
                            state.retrySeconds > 0 -> {
                                stringResource(R.string.retry_after, state.retrySeconds)
                            }
                            state.locked -> stringResource(R.string.booking_retry_same)
                            state.modifying -> stringResource(R.string.booking_modify_confirm)
                            else -> stringResource(R.string.booking_reserve)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingDetailRoute(
    reference: String,
    onBack: () -> Unit,
    onModify: (String, String) -> Unit = { _, _ -> },
    onDocument: (ByteArray) -> Unit = {},
    viewModel: BookingDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCancel by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(reference) { viewModel.load(reference) }
    LaunchedEffect(state.sending) { if (state.sending) showCancel = false }
    LaunchedEffect(state.document) {
        state.document?.let {
            onDocument(it)
            viewModel.documentHandled()
        }
    }
    BookingDetailScreen(
        state,
        onBack,
        { viewModel.load(reference) },
        { showCancel = true },
        viewModel::cancel,
        viewModel::resetRejected,
        viewModel::download,
        onModify
    )
    if (showCancel) {
        val keepFocus = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { if (!state.sending) showCancel = false },
            title = { Text(stringResource(R.string.booking_cancel_title)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.booking_cancel_warning))
                    OutlinedTextField(
                        state.reason,
                        viewModel::reasonChanged,
                        label = { Text(stringResource(R.string.booking_cancel_reason)) },
                        enabled = !state.locked,
                        isError = state.invalidReason,
                        supportingText = {
                            if (state.invalidReason) {
                                Text(stringResource(R.string.booking_reason_invalid))
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(viewModel::cancel, enabled = !state.sending) {
                    Text(stringResource(R.string.booking_cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancel = false },
                    modifier = Modifier.focusRequester(keepFocus)
                ) { Text(stringResource(R.string.booking_keep)) }
            }
        )
        LaunchedEffect(Unit) { keepFocus.requestFocus() }
    }
}

@Composable
fun BookingDetailScreen(
    state: BookingDetailState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onRequestCancel: () -> Unit,
    onRetryCancel: () -> Unit,
    onReset: () -> Unit,
    onDownload: () -> Unit,
    onModify: (String, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            TextButton(onBack, enabled = !state.sending) { Text(stringResource(R.string.back)) }
            BookingHeading(R.string.booking_detail_title)
        }
        if (state.busy) item { BookingLoading() }
        state.booking?.let { booking ->
            item {
                BookingStatusLabel(booking.status)
                Text(
                    stringResource(R.string.booking_reference, booking.reference),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(stringResource(R.string.booking_code, booking.confirmationCode))
            }
            item {
                Text(bookingDate(booking.startsAt), style = MaterialTheme.typography.titleLarge)
                Text(bookingDate(booking.endsAt))
                Text(stringResource(R.string.booking_places, booking.quantity))
                Text(
                    bookingMoney(booking.total, booking.currency),
                    style = MaterialTheme.typography.titleLarge
                )
                booking.deadline?.let {
                    Text(stringResource(R.string.booking_deadline, bookingDate(it)))
                }
                Text(stringResource(booking.nextActionLabel()))
            }
            booking.paymentInstructions?.takeIf { it.isNotBlank() }?.let { instructions ->
                item {
                    Text(
                        stringResource(R.string.booking_payment_instructions),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(instructions)
                }
            }
            item {
                HorizontalDivider()
                Text(
                    stringResource(R.string.booking_policy_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(booking.policy.content)
                if (booking.penaltyPercent > 0) {
                    Text(stringResource(R.string.booking_applied_penalty, booking.penaltyPercent))
                }
            }
            if (booking.originalRef != null || booking.replacementRef != null) {
                item {
                    booking.originalRef?.let {
                        Text(stringResource(R.string.booking_original_reference, it))
                    }
                    booking.replacementRef?.let {
                        Text(stringResource(R.string.booking_replacement_reference, it))
                    }
                    booking.externalAdjustment?.let {
                        Text(
                            stringResource(
                                R.string.booking_external_adjustment,
                                bookingMoney(it, booking.currency)
                            )
                        )
                    }
                }
            }
            if (booking.status in setOf(
                    BookingStatus.SOLICITADA,
                    BookingStatus.CONFIRMADA,
                    BookingStatus.PENDIENTE_PAGO_EXTERNO
                )
            ) {
                item {
                    OutlinedButton(
                        onDownload,
                        enabled = !state.downloading && !state.sending && !state.busy
                    ) {
                        Text(
                            stringResource(
                                if (state.downloading) {
                                    R.string.booking_document_loading
                                } else {
                                    R.string.booking_download
                                }
                            )
                        )
                    }
                    if (booking.status != BookingStatus.CONFIRMADA) {
                        Text(stringResource(R.string.booking_preliminary_document))
                    }
                }
            }
            if (booking.status in setOf(BookingStatus.SOLICITADA, BookingStatus.CONFIRMADA) &&
                booking.startsAt > Instant.now() &&
                !state.locked
            ) {
                item {
                    OutlinedButton(
                        onClick = { onModify(booking.serviceRef, booking.reference) },
                        enabled = !state.sending && !state.busy
                    ) { Text(stringResource(R.string.booking_modify_title)) }
                }
            }
            if (booking.canCancel(Instant.now()) && !state.locked) {
                item {
                    OutlinedButton(onRequestCancel, enabled = !state.sending && !state.busy) {
                        Text(stringResource(R.string.booking_cancel_confirm))
                    }
                }
            }
        }
        state.error?.let { error ->
            item {
                ErrorNotice(error)
                if (state.locked) {
                    if (error.kind in definitiveRejections) {
                        OutlinedButton(onReset, enabled = state.retrySeconds == 0L) {
                            Text(stringResource(R.string.booking_edit_reason))
                        }
                    } else {
                        Text(stringResource(R.string.booking_cancel_uncertain))
                        Button(
                            onRetryCancel,
                            enabled = !state.sending && state.retrySeconds == 0L
                        ) {
                            Text(
                                if (state.retrySeconds > 0) {
                                    stringResource(R.string.retry_after, state.retrySeconds)
                                } else {
                                    stringResource(R.string.booking_retry_same)
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onReload, enabled = !state.busy && !state.sending) {
                Text(stringResource(R.string.booking_refresh))
            }
        }
    }
}

@Composable
private fun BookingHeading(resource: Int) {
    Text(
        stringResource(resource),
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun BookingLoading() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(24.dp))
        Text(stringResource(R.string.booking_loading))
    }
}

@Composable
private fun BookingStatusLabel(status: BookingStatus) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            stringResource(status.label()),
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

internal fun BookingStatus.label(): Int = when (this) {
    BookingStatus.SOLICITADA -> R.string.booking_status_requested
    BookingStatus.PENDIENTE_PAGO_EXTERNO -> R.string.booking_status_payment
    BookingStatus.PENDIENTE_CONFIRMACION_PRESTADOR -> R.string.booking_status_approval
    BookingStatus.CONFIRMADA -> R.string.booking_status_confirmed
    BookingStatus.COMPLETADA -> R.string.booking_status_completed
    BookingStatus.CANCELADA -> R.string.booking_status_cancelled
    BookingStatus.EXPIRADA -> R.string.booking_status_expired
    BookingStatus.RECHAZADA -> R.string.booking_status_rejected
}

private fun BookingGroup.label(): Int = when (this) {
    BookingGroup.ACTIVAS -> R.string.booking_group_active
    BookingGroup.PASADAS -> R.string.booking_group_past
    BookingGroup.CANCELADAS -> R.string.booking_group_cancelled
}

private fun Booking.nextActionLabel(): Int = when (nextAction) {
    "CONTACTAR_PRESTADOR" -> R.string.booking_action_contact
    "ESPERAR_CONFIRMACION" -> R.string.booking_action_wait
    "DESCARGAR_COMPROBANTE" -> R.string.booking_action_confirmed
    "CONSULTAR_HISTORIAL" -> R.string.booking_action_history
    "CREAR_NUEVA_RESERVA" -> R.string.booking_action_new
    else -> R.string.booking_action_check
}

@Composable
private fun bookingDate(value: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(LocalConfiguration.current.locales[0])
        .withZone(ZoneId.of("America/Bogota")).format(value)

@Composable
private fun bookingMoney(value: BigDecimal, currencyCode: String): String {
    val locale: Locale = LocalConfiguration.current.locales[0]
    return NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(currencyCode)
    }.format(value)
}
