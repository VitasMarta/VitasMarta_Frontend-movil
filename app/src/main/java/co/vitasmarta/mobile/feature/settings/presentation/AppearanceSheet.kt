package co.vitasmarta.mobile.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.preferences.ThemeMode

internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(state: AppearanceState, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val systemLabel = if (systemDark) R.string.theme_dark else R.string.theme_light
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.appearance),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.appearance_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ThemeMode.entries.forEach { mode ->
                val selected = mode == state.mode
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 72.dp)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            MaterialTheme.shapes.medium
                        )
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            onSelect(mode)
                        })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        when (mode) {
                            ThemeMode.System -> Icons.Outlined.BrightnessAuto
                            ThemeMode.Light -> Icons.Outlined.LightMode
                            ThemeMode.Dark -> Icons.Outlined.DarkMode
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(mode.labelRes()),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (mode == ThemeMode.System) {
                            Text(
                                stringResource(
                                    R.string.theme_system_now,
                                    stringResource(systemLabel)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(selected = selected, onClick = null)
                }
            }
            if (state.storageError) {
                Text(
                    stringResource(R.string.theme_storage_error),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}
