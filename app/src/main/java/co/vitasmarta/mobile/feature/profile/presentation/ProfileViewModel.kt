package co.vitasmarta.mobile.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.feature.profile.domain.ConsentEvidence
import co.vitasmarta.mobile.feature.profile.domain.InterestCategory
import co.vitasmarta.mobile.feature.profile.domain.MemberProfile
import co.vitasmarta.mobile.feature.profile.domain.ProfileConsents
import co.vitasmarta.mobile.feature.profile.domain.ProfilePreferences
import co.vitasmarta.mobile.feature.profile.domain.ProfileRepository
import co.vitasmarta.mobile.feature.profile.domain.RightsCase
import co.vitasmarta.mobile.feature.profile.domain.RightsRequest
import co.vitasmarta.mobile.feature.profile.domain.RightsType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProfileSection { Overview, Personal, Language, Interests, Privacy, Rights, RightsDetail }

data class ProfileState(
    val section: ProfileSection = ProfileSection.Overview,
    val profile: MemberProfile? = null,
    val name: String = "",
    val invalidName: Boolean = false,
    val preferences: ProfilePreferences? = null,
    val language: String = "es-CO",
    val consents: ProfileConsents? = null,
    val categories: List<InterestCategory>? = null,
    val selectedInterests: Set<String> = emptySet(),
    val interestsSaved: Boolean = false,
    val cases: List<RightsCase>? = null,
    val caseDetail: RightsCase? = null,
    val caseReference: String? = null,
    val receipt: String? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val saved: Boolean = false,
    val retrySeconds: Long = 0,
    val error: AppError? = null
) {
    override fun toString() =
        "ProfileState(section=$section, loading=$loading, busy=$busy, redacted)"
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val sessions: SessionStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var operation: Job? = null
    private var cooldown: Job? = null

    init {
        viewModelScope.launch {
            sessions.state.collect { session ->
                revision++
                operation?.cancel()
                cooldown?.cancel()
                mutableState.value = ProfileState(loading = false)
                if (session is SessionState.Authenticated) reload()
            }
        }
    }

    fun open(section: ProfileSection) {
        if (mutableState.value.busy) return
        operation?.cancel()
        mutableState.value = mutableState.value.copy(
            section = section,
            error = null,
            saved = false,
            invalidName = false,
            loading = false
        )
        reload()
    }

    fun nameChanged(value: String) {
        if (!mutableState.value.busy) {
            mutableState.value = mutableState.value.copy(
                name = value,
                invalidName = false,
                saved = false,
                error = null
            )
        }
    }
    fun languageChanged(value: String) {
        if (!mutableState.value.busy && value in setOf("es-CO", "en-US")) {
            mutableState.value =
                mutableState.value.copy(language = value, saved = false, error = null)
        }
    }
    fun toggleInterest(slug: String) {
        val current = mutableState.value
        if (current.busy || current.categories?.none { it.slug == slug } != false) return
        mutableState.value = current.copy(
            selectedInterests = if (slug in current.selectedInterests) {
                current.selectedInterests - slug
            } else {
                current.selectedInterests + slug
            },
            saved = false,
            error = null
        )
    }

    fun reload() {
        if (mutableState.value.busy) return
        val ticket = ++revision
        operation?.cancel()
        val section = mutableState.value.section
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        operation = viewModelScope.launch {
            try {
                when (section) {
                    ProfileSection.Overview, ProfileSection.Personal -> {
                        when (val result = fresh { repository.profile() }) {
                            is AppResult.Success -> mutableState.value = mutableState.value.copy(
                                profile = result.value,
                                name = result.value.visibleName.orEmpty()
                            )
                            is AppResult.Failure -> fail(result.error)
                        }
                    }
                    ProfileSection.Language -> when (
                        val result = fresh {
                            repository.preferences()
                        }
                    ) {
                        is AppResult.Success -> mutableState.value = mutableState.value.copy(
                            preferences = result.value,
                            language = result.value.preferredLanguage
                        )
                        is AppResult.Failure -> fail(result.error)
                    }
                    ProfileSection.Privacy -> when (val result = fresh { repository.consents() }) {
                        is AppResult.Success ->
                            mutableState.value =
                                mutableState.value.copy(consents = result.value)
                        is AppResult.Failure -> fail(result.error)
                    }
                    ProfileSection.Interests -> when (
                        val result = fresh {
                            repository.categories()
                        }
                    ) {
                        is AppResult.Success ->
                            mutableState.value =
                                mutableState.value.copy(categories = result.value)
                        is AppResult.Failure -> fail(result.error)
                    }
                    ProfileSection.Rights -> when (
                        val result = fresh {
                            repository.rightsCases()
                        }
                    ) {
                        is AppResult.Success ->
                            mutableState.value =
                                mutableState.value.copy(cases = result.value)
                        is AppResult.Failure -> fail(result.error)
                    }
                    ProfileSection.RightsDetail -> {
                        val reference = mutableState.value.caseReference
                        if (reference == null) {
                            fail(AppError(ErrorKind.InvalidResponse))
                        } else {
                            when (val result = fresh { repository.rightsCase(reference) }) {
                                is AppResult.Success ->
                                    mutableState.value =
                                        mutableState.value.copy(
                                            caseDetail = result.value
                                        )
                                is AppResult.Failure -> fail(result.error)
                            }
                        }
                    }
                }
            } finally {
                if (revision == ticket) {
                    mutableState.value = mutableState.value.copy(loading = false)
                }
            }
        }
    }

    private suspend fun <T> fresh(block: suspend () -> AppResult<T>): AppResult<T> {
        val before = sessions.snapshot()
        val result = block()
        currentCoroutineContext().ensureActive()
        return if (sessions.snapshot().generation == before.generation && before.tokens != null) {
            result
        } else {
            AppResult.Failure(AppError(ErrorKind.Unauthorized))
        }
    }
    private fun fail(error: AppError) {
        val retry = error.retryAfterSeconds?.coerceAtLeast(0) ?: 0
        mutableState.value = mutableState.value.copy(error = error, retrySeconds = retry)
        cooldown?.cancel()
        if (retry > 0) {
            cooldown = viewModelScope.launch {
                while (mutableState.value.retrySeconds > 0) {
                    delay(1000)
                    mutableState.value = mutableState.value.copy(
                        retrySeconds = (mutableState.value.retrySeconds - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }
    private fun mutate(block: suspend () -> Unit) {
        if (mutableState.value.busy ||
            mutableState.value.loading ||
            mutableState.value.retrySeconds > 0
        ) {
            return
        }
        val ticket = ++revision
        mutableState.value = mutableState.value.copy(busy = true, saved = false, error = null)
        operation = viewModelScope.launch {
            try {
                block()
            } finally {
                if (revision == ticket) {
                    mutableState.value = mutableState.value.copy(busy = false)
                }
            }
        }
    }
    fun saveName() {
        val name = mutableState.value.name.trim()
        if (name.isBlank()) {
            mutableState.value = mutableState.value.copy(invalidName = true)
            return
        }
        mutate {
            when (val result = fresh { repository.updateName(name) }) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(
                    profile = mutableState.value.profile?.let {
                        MemberProfile(it.userId, name, null)
                    },
                    name = name,
                    saved = true
                )
                is AppResult.Failure -> fail(result.error)
            }
        }
    }
    fun saveLanguage(onConfirmed: (String) -> Unit) {
        val value = mutableState.value.language
        if (mutableState.value.preferences == null) return
        mutate {
            when (val result = fresh { repository.updateLanguage(value) }) {
                is AppResult.Success -> {
                    mutableState.value = mutableState.value.copy(
                        preferences = result.value,
                        language = result.value.preferredLanguage,
                        saved = true
                    )
                    onConfirmed(result.value.preferredLanguage)
                }
                is AppResult.Failure -> fail(result.error)
            }
        }
    }
    fun saveInterests() {
        val selected = mutableState.value.selectedInterests.toList()
        if (mutableState.value.categories == null) return
        mutate {
            when (val result = fresh { repository.replaceInterests(selected) }) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(
                    saved = true,
                    interestsSaved = true
                )
                is AppResult.Failure -> fail(result.error)
            }
        }
    }
    fun changeConsent(purpose: String, granted: Boolean) {
        if (mutableState.value.consents == null) return
        mutate {
            when (val result = fresh { repository.setConsent(purpose, granted) }) {
                is AppResult.Success -> {
                    val current = mutableState.value.consents?.current.orEmpty()
                    mutableState.value = mutableState.value.copy(
                        consents = ProfileConsents(
                            current + (purpose to ConsentEvidence(granted, null, null))
                        ),
                        saved = true
                    )
                }
                is AppResult.Failure -> fail(result.error)
            }
        }
    }
    fun openCase(reference: String) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(
            section = ProfileSection.RightsDetail,
            caseReference = reference,
            caseDetail = null,
            error = null
        )
        reload()
    }
    fun submitRights(request: RightsRequest) {
        mutate {
            when (val result = fresh { repository.submitRights(request) }) {
                is AppResult.Success -> {
                    if (request.type == RightsType.CANCELACION) {
                        sessions.clear()
                    } else {
                        mutableState.value =
                            mutableState.value.copy(receipt = result.value.reference, saved = true)
                        when (val list = fresh { repository.rightsCases() }) {
                            is AppResult.Success ->
                                mutableState.value =
                                    mutableState.value.copy(cases = list.value)
                            is AppResult.Failure -> fail(list.error)
                        }
                    }
                }
                is AppResult.Failure -> fail(result.error)
            }
        }
    }
    fun exportData(onDownloaded: (ByteArray) -> Unit) {
        mutate {
            when (val result = fresh { repository.exportData() }) {
                is AppResult.Success -> onDownloaded(result.value)
                is AppResult.Failure -> fail(result.error)
            }
        }
    }
}
