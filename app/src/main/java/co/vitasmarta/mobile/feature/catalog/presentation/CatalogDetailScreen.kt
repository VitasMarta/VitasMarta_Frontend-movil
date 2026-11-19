package co.vitasmarta.mobile.feature.catalog.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.designsystem.ErrorNotice
import coil3.compose.AsyncImage

@Composable
fun CatalogDetailRoute(
    id: String,
    onBack: () -> Unit,
    onBook: (String) -> Unit,
    viewModel: CatalogDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val language = if (locale.language == "en") "en-US" else "es-CO"
    LaunchedEffect(id, language) { viewModel.open(id, language) }
    CatalogDetailScreen(state, viewModel::reload, onBack, onBook)
}

@Composable
fun CatalogDetailScreen(
    state: CatalogDetailState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onBook: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
        if (state.loading) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text(stringResource(R.string.catalog_loading_detail))
                }
            }
        }
        state.error?.let { error ->
            item {
                if (error.kind == ErrorKind.NotFound) {
                    Text(
                        stringResource(R.string.catalog_detail_not_found),
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    ErrorNotice(error, onRetry = onRetry)
                }
            }
        }
        state.detail?.let { detail ->
            item {
                Text(
                    detail.name ?: stringResource(R.string.catalog_detail_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                if (detail.categories.isNotEmpty()) {
                    Text(
                        detail.categories.joinToString(", "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            items(detail.media, key = { "${it.order}:${it.url}" }) { media ->
                if (media.kind.equals("video", ignoreCase = true)) {
                    TextButton(onClick = {
                        linkError = !runCatching { uriHandler.openUri(media.url) }.isSuccess
                    }) { Text(stringResource(R.string.catalog_open_video)) }
                } else {
                    Surface(shape = MaterialTheme.shapes.large) {
                        CatalogPhoto(
                            media.url,
                            detail.name,
                            Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                }
            }
            if (linkError) {
                item { Text(stringResource(R.string.catalog_open_link_error)) }
            }
            if (detail.fromCache || detail.freshnessWarning) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            stringResource(R.string.catalog_freshness_warning),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            detail.description?.let { description ->
                item {
                    CatalogSectionTitle(stringResource(R.string.catalog_about))
                    Text(description, modifier = Modifier.padding(top = 12.dp))
                }
            }
            if (detail.minimumPrice != null || detail.maximumPrice != null) {
                item {
                    CatalogSectionTitle(stringResource(R.string.catalog_reference_price))
                    Text(
                        listOfNotNull(
                            detail.minimumPrice?.toPlainString(),
                            detail.maximumPrice?.takeIf { it != detail.minimumPrice }
                                ?.toPlainString()
                        ).joinToString(" – ") + detail.currency?.let { " $it" }.orEmpty(),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        stringResource(R.string.catalog_price_booking_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            if (detail.attributes.isNotEmpty()) {
                item { CatalogSectionTitle(stringResource(R.string.catalog_information)) }
                items(detail.attributes, key = { it.name }) { attribute ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            attribute.name.replace('_', ' '),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(attribute.value)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            detail.location?.let { location ->
                item {
                    CatalogSectionTitle(stringResource(R.string.catalog_location))
                    Text(
                        stringResource(
                            R.string.catalog_coordinates,
                            location.latitude.toString(),
                            location.longitude.toString()
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    TextButton(onClick = {
                        linkError = !runCatching {
                            uriHandler.openUri("geo:${location.latitude},${location.longitude}")
                        }.isSuccess
                    }) { Text(stringResource(R.string.catalog_open_map)) }
                }
            }
            item {
                CatalogSectionTitle(stringResource(R.string.catalog_availability))
                Text(
                    stringResource(R.string.catalog_slots_informational),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (state.slotsLoading) {
                item { CircularProgressIndicator(Modifier.size(24.dp)) }
            }
            state.slotsError?.let { error ->
                item { ErrorNotice(error, onRetry = onRetry) }
            }
            if (!state.slotsLoading && state.slots.isEmpty() && state.slotsError == null) {
                item { Text(stringResource(R.string.catalog_no_slots)) }
            }
            items(state.slots) { slot ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(slot.date, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.catalog_slot_times, slot.start, slot.end))
                    Text(stringResource(R.string.catalog_slot_capacity, slot.capacity))
                    Text(slot.status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (slot.withdrawn) Text(stringResource(R.string.catalog_slot_withdrawn))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            item {
                Button(
                    onClick = { onBook(detail.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text(stringResource(R.string.catalog_booking_options)) }
            }
        }
    }
}

@Composable
private fun CatalogSectionTitle(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
internal fun CatalogPhoto(url: String?, description: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        if (url != null) {
            // Coil's own image client does not receive the Gateway Authorization interceptor.
            AsyncImage(
                model = url,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
