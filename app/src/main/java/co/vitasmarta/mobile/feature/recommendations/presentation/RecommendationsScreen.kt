package co.vitasmarta.mobile.feature.recommendations.presentation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.designsystem.ErrorNotice
import java.text.NumberFormat

@Composable
fun RecommendationsRoute(
    onAttraction: (String) -> Unit,
    onProfile: (() -> Unit)? = null,
    viewModel: RecommendationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecommendationsScreen(state, viewModel::reload, onAttraction, onProfile)
}

@Composable
fun RecommendationsScreen(
    state: RecommendationsState,
    onReload: () -> Unit,
    onAttraction: (String) -> Unit,
    onProfile: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(color = colors.secondaryContainer, shape = MaterialTheme.shapes.large) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        null,
                        tint = colors.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp).size(28.dp)
                    )
                }
                Text(
                    stringResource(R.string.reco_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        if (state.loading) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(stringResource(R.string.reco_loading))
                }
            }
        }
        state.error?.let { error -> item { ErrorNotice(error, onRetry = onReload) } }
        state.feed?.let { feed ->
            item {
                Text(
                    stringResource(
                        if (feed.personalized) {
                            R.string.reco_personalized
                        } else {
                            R.string.reco_standard
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (feed.degraded) {
                item {
                    Surface(
                        color = colors.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.Info, null, tint = colors.secondary)
                            Text(
                                stringResource(R.string.reco_degraded),
                                modifier = Modifier.semantics {
                                    liveRegion = LiveRegionMode.Polite
                                }
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(
                            if (feed.generatedByAi) R.string.reco_ai else R.string.reco_rules
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (feed.transparency.isNotBlank()) {
                        Text(
                            feed.transparency,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (!feed.environmentalStateVerified) {
                        Text(
                            stringResource(R.string.reco_environment_unknown),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    feed.warnings.filter(String::isNotBlank).forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (feed.items.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            stringResource(R.string.reco_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.reco_empty_body),
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            items(feed.items, key = { it.id }) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (item.explanation.isNotBlank()) Text(item.explanation)
                    item.saturation?.let { saturation ->
                        Text(
                            stringResource(
                                R.string.reco_saturation,
                                stringResource(saturationLabel(saturation))
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item.occupancy?.let { occupancy ->
                        Text(
                            stringResource(
                                R.string.reco_occupancy,
                                NumberFormat.getNumberInstance().format(occupancy)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item.environmentalWarning?.takeIf(String::isNotBlank)?.let {
                        Text(it, color = colors.onSurfaceVariant)
                    }
                    Button(
                        onClick = { onAttraction(item.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    ) {
                        Text(stringResource(R.string.reco_open_attraction))
                    }
                    item.alternatives.distinct().forEach { alternative ->
                        TextButton(onClick = { onAttraction(alternative) }) {
                            Text(
                                feed.items.firstOrNull { it.id == alternative }?.name
                                    ?: stringResource(R.string.reco_open_alternative)
                            )
                        }
                    }
                    HorizontalDivider(color = colors.outlineVariant)
                }
            }
            onProfile?.let { open ->
                item {
                    TextButton(onClick = open) {
                        Text(stringResource(R.string.reco_edit_interests))
                    }
                }
            }
            item {
                TextButton(onClick = onReload) { Text(stringResource(R.string.reco_refresh)) }
            }
        }
    }
}

private fun saturationLabel(value: String) = when (value) {
    "NOMINAL" -> R.string.reco_saturation_nominal
    "RECOMENDACION" -> R.string.reco_saturation_recommendation
    "PREVENTIVO" -> R.string.reco_saturation_preventive
    "CRITICO" -> R.string.reco_saturation_critical
    else -> R.string.reco_saturation_unknown
}
