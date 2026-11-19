@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package co.vitasmarta.mobile.feature.catalog.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.designsystem.ErrorNotice
import co.vitasmarta.mobile.feature.catalog.domain.CatalogOrder

@Composable
fun CatalogSearchRoute(
    initialCategory: String?,
    onBack: () -> Unit,
    onDetail: (String) -> Unit,
    viewModel: CatalogSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialCategory) { viewModel.open(initialCategory) }
    CatalogSearchScreen(
        state, viewModel::queryChanged, viewModel::submit, viewModel::applyFilters,
        viewModel::filterEdited,
        viewModel::clearFilters, viewModel::loadMore, onBack, onDetail
    )
}

@Composable
fun CatalogSearchScreen(
    state: CatalogSearchState,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onApplyFilters: (List<String>, CatalogOrder, Boolean, String, String) -> Boolean,
    onFilterEdited: () -> Unit,
    onClear: () -> Unit,
    onMore: () -> Unit,
    onBack: () -> Unit,
    onDetail: (String) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0].toLanguageTag()
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.criteria) { listState.scrollToItem(0) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(
                stringResource(R.string.catalog_results_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.query, onValueChange = onQuery,
                    label = { Text(stringResource(R.string.catalog_search_text)) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    isError = state.queryError,
                    supportingText = {
                        if (state.queryError) Text(stringResource(R.string.catalog_query_too_long))
                    },
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Search, null) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        onSearch()
                    })
                )
                FilledTonalIconButton(
                    onClick = {
                        keyboard?.hide()
                        showFilters = true
                    },
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.FilterList,
                        stringResource(R.string.filters)
                    )
                }
            }
            TextButton(onClick = {
                keyboard?.hide()
                onSearch()
            }, enabled = !state.loading) {
                Text(stringResource(R.string.catalog_search_action))
            }
        }
        if (state.criteria.categories.isNotEmpty()) {
            item {
                Column {
                    state.criteria.categories.forEach { slug ->
                        val name = state.categories.firstOrNull { it.slug == slug }
                            ?.displayName(locale) ?: slug
                        AssistChip(onClick = { showFilters = true }, label = { Text(name) })
                    }
                }
            }
        }
        if (state.loading) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(stringResource(R.string.catalog_loading_results))
                }
            }
        }
        state.error?.let { error ->
            item { ErrorNotice(error, onRetry = onSearch) }
        }
        state.page?.let { page ->
            item {
                Column(
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.catalog_results_count, page.total),
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (page.fromCache) {
                        Text(stringResource(R.string.catalog_cached_results))
                    }
                }
            }
            if (page.items.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            stringResource(R.string.catalog_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(stringResource(R.string.catalog_empty_body))
                        if (state.criteria.filtered) {
                            Button(onClick = onClear) {
                                Text(stringResource(R.string.clear_filters))
                            }
                        } else {
                            Button(onClick = onSearch) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
            items(page.items, key = { it.id }) { item ->
                Card(
                    onClick = { onDetail(item.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    CatalogPhoto(
                        item.imageUrl,
                        null,
                        Modifier.fillMaxWidth().heightIn(140.dp, 180.dp)
                    )
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            item.displayName(locale),
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (item.categories.isNotEmpty()) {
                            Text(
                                item.categories.joinToString(", "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item.minimumPrice?.let {
                            Text(
                                stringResource(R.string.catalog_reference_from, it.toPlainString())
                            )
                        }
                        Text(
                            stringResource(R.string.catalog_open_detail),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            state.moreError?.let { error ->
                item {
                    ErrorNotice(error, onRetry = if (page.nextCursor != null) onMore else onSearch)
                }
            }
            if (page.nextCursor != null) {
                item {
                    Button(
                        onClick = onMore,
                        enabled = !state.loadingMore,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    ) {
                        Text(
                            stringResource(
                                if (state.loadingMore) {
                                    R.string.catalog_loading_more
                                } else {
                                    R.string.catalog_load_more
                                }
                            )
                        )
                    }
                }
            }
        }
    }
    if (showFilters) {
        CatalogFiltersSheet(state, onApplyFilters, onFilterEdited) { showFilters = false }
    }
}

@Composable
private fun CatalogFiltersSheet(
    state: CatalogSearchState,
    onApply: (List<String>, CatalogOrder, Boolean, String, String) -> Boolean,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(state.criteria.categories) }
    var order by remember { mutableStateOf(state.criteria.order) }
    var descending by remember { mutableStateOf(state.criteria.descending) }
    var minimum by remember {
        mutableStateOf(state.criteria.minimumPrice?.toPlainString().orEmpty())
    }
    var maximum by remember {
        mutableStateOf(state.criteria.maximumPrice?.toPlainString().orEmpty())
    }
    val locale = LocalConfiguration.current.locales[0].toLanguageTag()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.filters), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.catalog_filter_categories),
                style = MaterialTheme.typography.titleMedium
            )
            state.categories.forEach { category ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(
                        category.slug in selected,
                        role = Role.Checkbox,
                        onValueChange = {
                            selected = if (it) {
                                selected + category.slug
                            } else {
                                selected - category.slug
                            }
                        }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(category.slug in selected, onCheckedChange = null)
                    Text(category.displayName(locale))
                }
            }
            Text(
                stringResource(R.string.catalog_sort_title),
                style = MaterialTheme.typography.titleMedium
            )
            CatalogOrder.entries.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                        order == option,
                        role = Role.RadioButton,
                        onClick = { order = option }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(order == option, onClick = null)
                    Text(stringResource(option.label()))
                }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                    descending,
                    role = Role.Checkbox,
                    onValueChange = { descending = it }
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(descending, onCheckedChange = null)
                Text(stringResource(R.string.catalog_descending))
            }
            OutlinedTextField(
                minimum,
                {
                    minimum = it
                    onEdit()
                },
                label = { Text(stringResource(R.string.catalog_price_min)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.filterError
            )
            OutlinedTextField(
                maximum,
                {
                    maximum = it
                    onEdit()
                },
                label = { Text(stringResource(R.string.catalog_price_max)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.filterError
            )
            if (state.filterError) {
                Text(
                    stringResource(R.string.catalog_filter_price_error),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = {
                    if (onApply(selected, order, descending, minimum, maximum)) onDismiss()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) { Text(stringResource(R.string.apply_filters)) }
        }
    }
}

private fun CatalogOrder.label(): Int = when (this) {
    CatalogOrder.Relevance -> R.string.catalog_sort_relevance
    CatalogOrder.Name -> R.string.catalog_sort_name
    CatalogOrder.Price -> R.string.catalog_sort_price
    CatalogOrder.Popularity -> R.string.catalog_sort_popularity
    CatalogOrder.Sustainability -> R.string.catalog_sort_sustainability
}
