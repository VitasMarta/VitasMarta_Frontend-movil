package co.vitasmarta.mobile.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.designsystem.ErrorNotice

@Composable
fun RegistrationRoute(onBack: () -> Unit, viewModel: RegistrationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegistrationScreen(
        state,
        viewModel::emailChanged,
        viewModel::passwordChanged,
        viewModel::treatmentChanged,
        viewModel::commercialChanged,
        viewModel::profilingChanged,
        viewModel::register,
        onBack
    )
}

@Composable
fun RegistrationScreen(
    state: RegistrationState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTreatment: (Boolean) -> Unit,
    onCommercial: (Boolean) -> Unit,
    onProfiling: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val treatmentFocus = remember { FocusRequester() }
    var visible by remember { mutableStateOf(false) }
    val passwordHint = stringResource(R.string.registration_password_hint)
    val passwordErrorMessage = stringResource(R.string.registration_password_error)
    val emailErrorMessage = stringResource(R.string.invalid_email)
    val treatmentErrorMessage = stringResource(R.string.consent_treatment_error)
    LaunchedEffect(state.emailError, state.passwordError, state.treatmentError) {
        when {
            state.emailError -> emailFocus.requestFocus()
            state.passwordError -> passwordFocus.requestFocus()
            state.treatmentError -> treatmentFocus.requestFocus()
        }
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack, enabled = !state.busy) {
            Text(stringResource(R.string.back))
        }
        if (state.created) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    Icons.Outlined.MarkEmailRead,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(18.dp).size(32.dp)
                )
            }
            Text(
                stringResource(R.string.check_email),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.registration_created),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.login))
            }
        } else {
            Text(
                stringResource(R.string.create_account),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.registration_description))
            Text(
                stringResource(R.string.registration_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.email, onValueChange = onEmail,
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth().focusRequester(emailFocus).semantics {
                    contentType = ContentType.NewUsername + ContentType.EmailAddress
                    if (state.emailError) error(emailErrorMessage)
                },
                enabled = !state.busy, singleLine = true, isError = state.emailError,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null) },
                supportingText = {
                    if (state.emailError) Text(stringResource(R.string.invalid_email))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() })
            )
            Text(
                passwordHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val visibility = stringResource(
                if (visible) R.string.hide_password else R.string.show_password
            )
            OutlinedTextField(
                value = state.password, onValueChange = onPassword,
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus).semantics {
                    contentType = ContentType.NewPassword
                    stateDescription = passwordHint
                    if (state.passwordError) error(passwordErrorMessage)
                },
                enabled = !state.busy, singleLine = true, isError = state.passwordError,
                shape = MaterialTheme.shapes.medium,
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                supportingText = {
                    if (state.passwordError) {
                        Text(stringResource(R.string.registration_password_error))
                    }
                },
                trailingIcon = {
                    IconToggleButton(
                        checked = visible,
                        onCheckedChange = { visible = it }
                    ) {
                        Icon(
                            if (visible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = visibility
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onNext = { treatmentFocus.requestFocus() })
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                stringResource(R.string.registration_consents),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            ConsentRow(
                stringResource(R.string.consent_treatment),
                state.treatment,
                !state.busy,
                onTreatment,
                Modifier.focusRequester(treatmentFocus)
            )
            if (state.treatmentError) {
                Text(
                    stringResource(R.string.consent_treatment_error),
                    color = MaterialTheme.colorScheme.error
                )
            }
            ConsentRow(
                stringResource(R.string.consent_commercial),
                state.commercial,
                !state.busy,
                onCommercial
            )
            ConsentRow(
                stringResource(R.string.consent_profiling),
                state.profiling,
                !state.busy,
                onProfiling
            )
            state.error?.let { error ->
                if (error.kind in setOf(ErrorKind.Validation, ErrorKind.Conflict)) {
                    Text(
                        stringResource(R.string.registration_rejected),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                } else {
                    ErrorNotice(error)
                }
            }
            Button(
                onClick = onRegister,
                enabled = !state.busy && state.retrySeconds == 0L,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                if (state.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp).size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    if (state.retrySeconds > 0) {
                        stringResource(R.string.retry_after, state.retrySeconds)
                    } else {
                        stringResource(
                            if (state.busy) {
                                R.string.registration_pending
                            } else {
                                R.string.create_account
                            }
                        )
                    }
                )
            }
            TextButton(onClick = onBack, enabled = !state.busy) {
                Text(stringResource(R.string.already_have_account))
            }
        }
    }
}

@Composable
private fun ConsentRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(checked, enabled, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
