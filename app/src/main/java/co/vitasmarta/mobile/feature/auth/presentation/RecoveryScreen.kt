package co.vitasmarta.mobile.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.designsystem.ErrorNotice

@Composable
fun RecoveryRoute(onBack: () -> Unit, viewModel: RecoveryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecoveryScreen(state, viewModel::emailChanged, viewModel::recover, onBack)
}

@Composable
fun RecoveryScreen(
    state: RecoveryState,
    onEmail: (String) -> Unit,
    onRecover: () -> Unit,
    onBack: () -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.invalidEmail) { if (state.invalidEmail) focus.requestFocus() }
    Column(
        Modifier.verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                Icons.Outlined.MarkEmailRead,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(18.dp).size(32.dp)
            )
        }
        Text(
            stringResource(if (state.accepted) R.string.check_email else R.string.recover_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() }
        )
        if (state.accepted) {
            Text(stringResource(R.string.recovery_accepted))
        } else {
            Text(stringResource(R.string.recovery_description))
            OutlinedTextField(
                value = state.email, onValueChange = onEmail,
                label = { Text(stringResource(R.string.email)) },
                singleLine = true, enabled = !state.busy, isError = state.invalidEmail,
                supportingText = {
                    if (state.invalidEmail) Text(stringResource(R.string.invalid_email))
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics {
                    contentType =
                        ContentType.EmailAddress
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onRecover() })
            )
            state.error?.let { ErrorNotice(it) }
            Button(
                shape = MaterialTheme.shapes.small,
                onClick = onRecover,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Text(
                    stringResource(
                        if (state.busy) R.string.recovery_sending else R.string.recovery_send
                    )
                )
            }
        }
    }
}
