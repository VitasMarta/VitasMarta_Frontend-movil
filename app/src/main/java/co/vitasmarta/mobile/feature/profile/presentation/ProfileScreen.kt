package co.vitasmarta.mobile.feature.profile.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.designsystem.ErrorNotice
import co.vitasmarta.mobile.feature.profile.domain.RightsRequest
import co.vitasmarta.mobile.feature.profile.domain.RightsType

class ProfileActions(
    val open: (ProfileSection) -> Unit,
    val nameChanged: (String) -> Unit,
    val languageChanged: (String) -> Unit,
    val toggleInterest: (String) -> Unit,
    val saveName: () -> Unit,
    val saveLanguage: () -> Unit,
    val saveInterests: () -> Unit,
    val changeConsent: (String, Boolean) -> Unit,
    val openCase: (String) -> Unit,
    val submitRights: (RightsRequest) -> Unit,
    val export: (() -> Unit)?,
    val logout: () -> Unit,
    val reload: () -> Unit
)

@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    onLanguageChanged: (String) -> Unit = {},
    onExport: ((ByteArray) -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.section != ProfileSection.Overview) {
        if (!state.busy) {
            viewModel.open(
                if (state.section == ProfileSection.RightsDetail) {
                    ProfileSection.Rights
                } else {
                    ProfileSection.Overview
                }
            )
        }
    }
    ProfileScreen(
        state,
        ProfileActions(
            viewModel::open, viewModel::nameChanged, viewModel::languageChanged,
            viewModel::toggleInterest, viewModel::saveName,
            { viewModel.saveLanguage(onLanguageChanged) }, viewModel::saveInterests,
            viewModel::changeConsent, viewModel::openCase, viewModel::submitRights,
            onExport?.let { callback -> { viewModel.exportData(callback) } },
            onLogout, viewModel::reload
        )
    )
}

@Composable
fun ProfileScreen(state: ProfileState, actions: ProfileActions) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0].toLanguageTag()
    var pendingConsent by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var confirmInterests by remember { mutableStateOf(false) }
    var pendingRight by remember { mutableStateOf<RightsRequest?>(null) }
    val enabled = !state.busy && !state.loading && state.retrySeconds == 0L
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (state.section != ProfileSection.Overview) {
            item {
                TextButton(onClick = {
                    actions.open(
                        if (state.section == ProfileSection.RightsDetail) {
                            ProfileSection.Rights
                        } else {
                            ProfileSection.Overview
                        }
                    )
                }, enabled = !state.busy) { Text(stringResource(R.string.back)) }
            }
        }
        item {
            Text(
                stringResource(sectionTitle(state.section)),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (state.loading || state.busy) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(
                        stringResource(
                            if (state.busy) R.string.member_saving else R.string.member_loading
                        )
                    )
                }
            }
        }
        state.error?.let { error -> item { ErrorNotice(error, onRetry = actions.reload) } }
        if (state.retrySeconds >
            0
        ) {
            item { Text(stringResource(R.string.retry_after, state.retrySeconds)) }
        }
        if (state.saved) {
            item {
                Text(
                    stringResource(R.string.member_saved),
                    color = colors.secondary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
        when (state.section) {
            ProfileSection.Overview -> {
                item {
                    Surface(color = colors.primaryContainer, shape = MaterialTheme.shapes.large) {
                        Row(
                            Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PersonOutline,
                                null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    state.profile?.visibleName?.takeIf(String::isNotBlank)
                                        ?: stringResource(R.string.member_welcome),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.onPrimaryContainer
                                )
                                Text(
                                    stringResource(R.string.member_overview_description),
                                    color = colors.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                item {
                    ProfileLink(
                        R.string.member_personal,
                        Icons.Outlined.PersonOutline,
                        !state.busy
                    ) {
                        actions.open(ProfileSection.Personal)
                    }
                }
                item {
                    ProfileLink(R.string.member_language, Icons.Outlined.Language, !state.busy) {
                        actions.open(ProfileSection.Language)
                    }
                }
                item {
                    ProfileLink(R.string.member_interests, Icons.Outlined.Interests, !state.busy) {
                        actions.open(ProfileSection.Interests)
                    }
                }
                item {
                    ProfileLink(R.string.member_privacy, Icons.Outlined.PrivacyTip, !state.busy) {
                        actions.open(ProfileSection.Privacy)
                    }
                }
                item {
                    ProfileLink(R.string.member_rights, Icons.Outlined.Shield, !state.busy) {
                        actions.open(ProfileSection.Rights)
                    }
                }
                item {
                    TextButton(onClick = actions.logout, enabled = !state.busy) {
                        Text(stringResource(R.string.logout))
                    }
                }
            }
            ProfileSection.Personal -> if (state.profile != null) {
                item {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(state.invalidName) {
                        if (state.invalidName) focus.requestFocus()
                    }
                    OutlinedTextField(
                        value = state.name, onValueChange = actions.nameChanged,
                        label = { Text(stringResource(R.string.member_visible_name)) },
                        enabled = enabled, singleLine = true, isError = state.invalidName,
                        supportingText = {
                            if (state.invalidName) {
                                Text(
                                    stringResource(R.string.member_name_required)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics {
                            contentType = ContentType.PersonFullName
                        }
                    )
                }
                item { MainAction(R.string.member_save_name, enabled, actions.saveName) }
            }
            ProfileSection.Language -> if (state.preferences != null) {
                item { Text(stringResource(R.string.member_language_description)) }
                for ((value, resource) in listOf(
                    "es-CO" to R.string.member_spanish,
                    "en-US" to R.string.member_english
                )) {
                    item {
                        ChoiceRow(stringResource(resource), state.language == value, enabled) {
                            actions.languageChanged(value)
                        }
                    }
                }
                item { MainAction(R.string.member_save_language, enabled, actions.saveLanguage) }
            }
            ProfileSection.Interests -> {
                item { Text(stringResource(R.string.member_interests_description)) }
                state.categories?.let { categories ->
                    if (categories.isEmpty()) {
                        item {
                            Text(stringResource(R.string.member_no_categories))
                        }
                    }
                    items(categories, key = { it.slug }) { category ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(
                                category.slug in state.selectedInterests,
                                enabled,
                                Role.Checkbox,
                                onValueChange = { actions.toggleInterest(category.slug) }
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                category.slug in state.selectedInterests,
                                null,
                                enabled = enabled
                            )
                            Text(category.displayName(locale), Modifier.padding(start = 12.dp))
                        }
                    }
                    item {
                        MainAction(R.string.member_replace_interests, enabled) {
                            confirmInterests =
                                true
                        }
                    }
                }
            }
            ProfileSection.Privacy -> {
                item { Text(stringResource(R.string.member_privacy_description)) }
                state.consents?.let { consents ->
                    for ((purpose, resource) in listOf(
                        "COMERCIAL" to R.string.member_marketing,
                        "PERFILADO" to R.string.member_profiling
                    )) {
                        item {
                            val value = consents.current[purpose]
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                        .toggleable(value?.granted == true, enabled, Role.Switch) {
                                            pendingConsent = purpose to it
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(stringResource(resource), Modifier.weight(1f))
                                    Switch(value?.granted == true, null, enabled = enabled)
                                }
                                if (value ==
                                    null
                                ) {
                                    Text(
                                        stringResource(R.string.member_consent_missing),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                value?.version?.let {
                                    Text(
                                        stringResource(R.string.member_policy_version, it),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            ProfileSection.Rights -> {
                item { Text(stringResource(R.string.member_rights_description)) }
                state.receipt?.let { reference ->
                    item {
                        Text(
                            stringResource(R.string.member_rights_receipt, reference),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
                item { RightsRequestForm(enabled, state.name) { pendingRight = it } }
                actions.export?.let { export ->
                    item {
                        Button(
                            onClick = export,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Outlined.Download, null, Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.member_export))
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.member_requests_history),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                state.cases?.let { cases ->
                    if (cases.isEmpty()) item { Text(stringResource(R.string.member_no_requests)) }
                    items(cases, key = { it.reference }) { request ->
                        Column(
                            Modifier.fillMaxWidth().clickable(enabled = enabled) {
                                actions.openCase(request.reference)
                            }.padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                rightTypeLabel(request.type),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(request.status)
                            Text(
                                request.receivedAt.orEmpty(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        HorizontalDivider(color = colors.outlineVariant)
                    }
                }
            }
            ProfileSection.RightsDetail -> state.caseDetail?.let { request ->
                item {
                    Text(rightTypeLabel(request.type), style = MaterialTheme.typography.titleLarge)
                }
                item { Text(request.status) }
                items(request.steps) { step ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(step.name, style = MaterialTheme.typography.titleMedium)
                        step.at?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        step.result?.let { Text(it) }
                        HorizontalDivider(color = colors.outlineVariant)
                    }
                }
            }
        }
    }
    if (confirmInterests) {
        ConfirmDialog(
            stringResource(R.string.member_replace_interests),
            stringResource(R.string.member_replace_warning),
            onDismiss = { confirmInterests = false },
            onConfirm = {
                confirmInterests = false
                actions.saveInterests()
            }
        )
    }
    pendingConsent?.let { (purpose, granted) ->
        ConfirmDialog(
            stringResource(R.string.member_privacy),
            stringResource(
                if (granted) R.string.member_consent_allow else R.string.member_consent_revoke,
                stringResource(
                    if (purpose ==
                        "COMERCIAL"
                    ) {
                        R.string.member_marketing
                    } else {
                        R.string.member_profiling
                    }
                )
            ),
            onDismiss = { pendingConsent = null },
            onConfirm = {
                pendingConsent = null
                actions.changeConsent(purpose, granted)
            }
        )
    }
    pendingRight?.let { request ->
        ConfirmDialog(
            rightTypeLabel(request.type.name),
            stringResource(
                if (request.type == RightsType.CANCELACION) {
                    R.string.member_cancellation_warning
                } else {
                    R.string.member_rights_confirm
                }
            ),
            onDismiss = { pendingRight = null },
            onConfirm = {
                pendingRight = null
                actions.submitRights(request)
            }
        )
    }
}

private fun sectionTitle(section: ProfileSection) = when (section) {
    ProfileSection.Overview -> R.string.member_profile
    ProfileSection.Personal -> R.string.member_personal
    ProfileSection.Language -> R.string.member_language
    ProfileSection.Interests -> R.string.member_interests
    ProfileSection.Privacy -> R.string.member_privacy
    ProfileSection.Rights, ProfileSection.RightsDetail -> R.string.member_rights
}

@Composable
private fun ProfileLink(label: Int, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Text(
                stringResource(label),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Icon(Icons.Outlined.ChevronRight, null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MainAction(label: Int, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) { Text(stringResource(label)) }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .selectable(selected, enabled, Role.RadioButton, onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected, null, enabled = enabled)
        Text(label, Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.member_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.member_cancel)) }
        }
    )
}

@Composable
private fun rightTypeLabel(type: String): String = stringResource(
    when (type) {
        "ACCESO" -> R.string.member_right_access
        "RECTIFICACION" -> R.string.member_right_rectification
        "OPOSICION" -> R.string.member_right_opposition
        "CANCELACION" -> R.string.member_right_cancellation
        else -> R.string.member_rights
    }
)

@Composable
private fun RightsRequestForm(enabled: Boolean, name: String, onRequest: (RightsRequest) -> Unit) {
    var type by remember { mutableStateOf(RightsType.ACCESO) }
    var correction by remember(name) { mutableStateOf(name) }
    var purpose by remember { mutableStateOf("COMERCIAL") }
    var invalid by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(invalid) { if (invalid) focus.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (option in RightsType.entries) {
            ChoiceRow(rightTypeLabel(option.name), type == option, enabled) {
                type = option
                invalid = false
            }
        }
        if (type == RightsType.RECTIFICACION) {
            OutlinedTextField(
                value = correction,
                onValueChange = {
                    correction = it
                    invalid = false
                },
                label = { Text(stringResource(R.string.member_visible_name)) },
                enabled = enabled,
                singleLine = true,
                isError = invalid,
                supportingText = {
                    if (invalid) Text(stringResource(R.string.member_name_required))
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        if (type == RightsType.OPOSICION) {
            ChoiceRow(stringResource(R.string.member_marketing), purpose == "COMERCIAL", enabled) {
                purpose =
                    "COMERCIAL"
            }
            ChoiceRow(stringResource(R.string.member_profiling), purpose == "PERFILADO", enabled) {
                purpose =
                    "PERFILADO"
            }
        }
        MainAction(R.string.member_submit_request, enabled) {
            if (type == RightsType.RECTIFICACION && correction.isBlank()) {
                invalid = true
            } else {
                onRequest(
                    RightsRequest(
                        type,
                        when (type) {
                            RightsType.RECTIFICACION -> correction.trim()
                            RightsType.OPOSICION -> purpose
                            else -> ""
                        }
                    )
                )
            }
        }
    }
}
