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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.designsystem.ErrorNotice

@Composable
fun AccountRoute(
    onRecover: () -> Unit,
    onRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    content: @Composable () -> Unit = {}
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (session is SessionState.Anonymous) {
        LoginScreen(
            state,
            viewModel::emailChanged,
            viewModel::passwordChanged,
            viewModel::login,
            onRecover,
            onRegister
        )
    } else {
        content()
    }
}

@Composable
fun LoginScreen(
    state: LoginState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onLogin: () -> Unit,
    onRecover: () -> Unit,
    onRegister: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    LaunchedEffect(state.emailError, state.passwordError) {
        if (state.emailError) {
            emailFocus.requestFocus()
        } else if (state.passwordError) {
            passwordFocus.requestFocus()
        }
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Outlined.PersonOutline,
                contentDescription = null,
                modifier = Modifier.padding(18.dp).size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(R.string.login_description))
        OutlinedButton(
            onClick = onRegister,
            enabled = !state.busy,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { Text(stringResource(R.string.create_account)) }
        OutlinedTextField(
            value = state.email, onValueChange = onEmail, label = {
                Text(stringResource(R.string.email))
            },
            modifier = Modifier.fillMaxWidth().focusRequester(emailFocus).semantics {
                contentType =
                    ContentType.Username + ContentType.EmailAddress
            },
            singleLine = true, enabled = !state.busy, isError = state.emailError,
            shape = MaterialTheme.shapes.medium,
            leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null) },
            supportingText = { if (state.emailError) Text(stringResource(R.string.invalid_email)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() })
        )
        OutlinedTextField(
            value = state.password, onValueChange = onPassword, label = {
                Text(stringResource(R.string.password))
            },
            modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus).semantics {
                contentType =
                    ContentType.Password
            },
            singleLine = true, enabled = !state.busy, isError = state.passwordError,
            shape = MaterialTheme.shapes.medium,
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            supportingText = {
                if (state.passwordError) Text(stringResource(R.string.required_password))
            },
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onLogin() }),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = stringResource(
                            if (visible) R.string.hide_password else R.string.show_password
                        )
                    )
                }
            }
        )
        state.error?.let { ErrorNotice(it) }
        Button(
            shape = MaterialTheme.shapes.small,
            onClick = onLogin,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(stringResource(if (state.busy) R.string.login_pending else R.string.login))
            }
        }
        TextButton(onClick = onRecover, enabled = !state.busy) {
            Text(stringResource(R.string.forgot_password))
        }
    }
}
