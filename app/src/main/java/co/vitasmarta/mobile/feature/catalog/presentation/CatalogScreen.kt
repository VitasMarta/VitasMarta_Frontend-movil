package co.vitasmarta.mobile.feature.catalog.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.designsystem.ErrorNotice
import co.vitasmarta.mobile.core.designsystem.TerritoryMark

@Composable
fun CatalogRoute(
    onCategory: (String) -> Unit,
    onSearch: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    CatalogScreen(
        state,
        filters,
        viewModel::queryChanged,
        viewModel::primaryChanged,
        viewModel::reload,
        onCategory,
        onSearch
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    state: CatalogState,
    filters: CatalogFilters,
    onQuery: (String) -> Unit,
    onPrimary: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onCategory: (String) -> Unit,
    onSearch: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0].toLanguageTag()
    val keyboard = LocalSoftwareKeyboardController.current
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.native_explore_title),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        stringResource(R.string.native_explore_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                TerritoryMark(Modifier.size(88.dp, 104.dp))
            }
        }
        item {
            OutlinedButton(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text(stringResource(R.string.catalog_search_attractions)) }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = filters.query, onValueChange = onQuery,
                    enabled = state is CatalogState.Content,
                    label = { Text(stringResource(R.string.search_categories)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(onClick = { onQuery("") }) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.clear_search))
                            }
                        }
                    },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() })
                )
                FilledTonalIconButton(
                    onClick = { showFilters = true },
                    enabled = state is CatalogState.Content,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Outlined.FilterList, stringResource(R.string.filters))
                }
            }
        }
        when (state) {
            CatalogState.Loading -> item {
                Row(
                    Modifier.padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(
                        stringResource(R.string.loading_categories),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is CatalogState.Error -> item {
                Surface(color = colors.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            stringResource(R.string.categories_unavailable),
                            style = MaterialTheme.typography.titleLarge
                        )
                        ErrorNotice(state.error, onRetry = onRetry, useErrorColor = false)
                    }
                }
            }
            is CatalogState.Content -> {
                item {
                    Text(
                        stringResource(R.string.categories_count, state.categories.size),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                if (state.categories.isEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                stringResource(
                                    if (filters.query.isNotBlank() || filters.primaryOnly) {
                                        R.string.no_categories_match
                                    } else {
                                        R.string.categories_empty
                                    }
                                ),
                                color = colors.onSurfaceVariant
                            )
                            if (filters.query.isNotBlank() || filters.primaryOnly) {
                                Button(onClick = {
                                    onQuery("")
                                    onPrimary(false)
                                }, shape = MaterialTheme.shapes.medium) {
                                    Text(stringResource(R.string.clear_filters))
                                }
                            }
                        }
                    }
                }
                items(state.categories, key = { it.slug }) { category ->
                    Column {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .testTag("catalog-category-${category.slug}")
                                .clickable(role = Role.Button) { onCategory(category.slug) }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = colors.secondaryContainer
                            ) {
                                Icon(
                                    Icons.Outlined.Sell,
                                    null,
                                    tint = colors.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp).size(24.dp)
                                )
                            }
                            Text(
                                category.displayName(locale),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null
                            )
                        }
                        HorizontalDivider(color = colors.outlineVariant)
                    }
                }
            }
        }
    }
    if (showFilters) {
        var primaryOnly by remember(filters.primaryOnly) { mutableStateOf(filters.primaryOnly) }
        ModalBottomSheet(onDismissRequest = {
            showFilters = false
        }, containerColor = colors.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    stringResource(R.string.filters),
                    style = MaterialTheme.typography.headlineMedium
                )
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .toggleable(primaryOnly, role = Role.Checkbox, onValueChange = {
                            primaryOnly =
                                it
                        }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(primaryOnly, onCheckedChange = null)
                    Text(
                        stringResource(R.string.only_primary_categories),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Button(
                    onClick = {
                        onPrimary(primaryOnly)
                        showFilters = false
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text(stringResource(R.string.apply_filters))
                }
            }
        }
    }
}
