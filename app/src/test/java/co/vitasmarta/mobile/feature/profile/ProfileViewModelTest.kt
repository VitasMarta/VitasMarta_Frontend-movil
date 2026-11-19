package co.vitasmarta.mobile.feature.profile

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.testToken
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
import co.vitasmarta.mobile.feature.profile.domain.RightsReceipt
import co.vitasmarta.mobile.feature.profile.domain.RightsRequest
import co.vitasmarta.mobile.feature.profile.domain.RightsType
import co.vitasmarta.mobile.feature.profile.presentation.ProfileSection
import co.vitasmarta.mobile.feature.profile.presentation.ProfileViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class Repository : ProfileRepository {
        var profileRead: suspend () -> AppResult<MemberProfile> = {
            AppResult.Success(MemberProfile("member", "Original", null))
        }
        var nameGate: CompletableDeferred<Unit>? = null
        var nameResult: AppResult<Unit> = AppResult.Success(Unit)
        val names = mutableListOf<String>()
        var consentGate: CompletableDeferred<Unit>? = null
        var consentResult: AppResult<Unit> = AppResult.Success(Unit)
        val consentWrites = mutableListOf<Pair<String, Boolean>>()
        val interestsWrites = mutableListOf<List<String>>()
        val languageWrites = mutableListOf<String>()
        var languageResult: AppResult<ProfilePreferences> =
            AppResult.Success(ProfilePreferences("en-US", "es-CO", "CORREO"))
        val rightsWrites = mutableListOf<RightsRequest>()
        var exportResult: AppResult<ByteArray> = AppResult.Failure(AppError(ErrorKind.Server))
        var caseFailure: AppError? = null
        var caseReads = 0

        override suspend fun profile() = profileRead()
        override suspend fun updateName(name: String): AppResult<Unit> {
            names += name
            nameGate?.await()
            return nameResult
        }
        override suspend fun preferences(): AppResult<ProfilePreferences> =
            AppResult.Success(ProfilePreferences("es-CO", "es-CO", "CORREO"))
        override suspend fun updateLanguage(language: String): AppResult<ProfilePreferences> {
            languageWrites += language
            return languageResult
        }
        override suspend fun consents(): AppResult<ProfileConsents> = AppResult.Success(
            ProfileConsents(
                mapOf(
                    "COMERCIAL" to ConsentEvidence(true, "1.0", null),
                    "PERFILADO" to ConsentEvidence(false, "1.0", null)
                )
            )
        )
        override suspend fun setConsent(purpose: String, granted: Boolean): AppResult<Unit> {
            consentWrites += purpose to granted
            consentGate?.await()
            return consentResult
        }
        override suspend fun categories(): AppResult<List<InterestCategory>> = AppResult.Success(
            listOf(InterestCategory("cultura", mapOf("es" to "Cultura")))
        )
        override suspend fun replaceInterests(slugs: List<String>): AppResult<Unit> {
            interestsWrites += slugs
            return AppResult.Success(Unit)
        }
        override suspend fun rightsCases(): AppResult<List<RightsCase>> =
            AppResult.Success(emptyList())
        override suspend fun rightsCase(reference: String): AppResult<RightsCase> {
            caseReads++
            return caseFailure?.let { AppResult.Failure(it) } ?: AppResult.Success(
                RightsCase(reference, "ACCESO", "EVIDENCIADA", null, emptyList())
            )
        }
        override suspend fun submitRights(request: RightsRequest): AppResult<RightsReceipt> {
            rightsWrites += request
            return AppResult.Success(RightsReceipt("request-1"))
        }
        override suspend fun exportData() = exportResult
    }

    private fun sessions() = SessionStore().also {
        check(it.install(TokenPair(testToken(), "refresh"), it.snapshot().generation))
    }

    @Test fun nameValidationAndSavingAreExplicitAndNeverOptimistic() = runTest {
        val repository = Repository()
        val viewModel = ProfileViewModel(repository, sessions())
        runCurrent()
        viewModel.open(ProfileSection.Personal)
        runCurrent()
        viewModel.nameChanged(" ")
        viewModel.saveName()
        assertTrue(viewModel.state.value.invalidName)
        assertTrue(repository.names.isEmpty())
        repository.nameGate = CompletableDeferred()
        viewModel.nameChanged(" Nuevo nombre ")
        viewModel.saveName()
        viewModel.saveName()
        runCurrent()
        assertEquals(listOf("Nuevo nombre"), repository.names)
        assertEquals("Original", viewModel.state.value.profile?.visibleName)
        assertFalse(viewModel.state.value.saved)
        assertTrue(viewModel.state.value.busy)
        repository.nameGate?.complete(Unit)
        runCurrent()
        assertEquals("Nuevo nombre", viewModel.state.value.profile?.visibleName)
        assertTrue(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.busy)
    }

    @Test fun consentChangesAreIndependentAndAppearOnlyAfterServerConfirmation() = runTest {
        val repository = Repository()
        val viewModel = ProfileViewModel(repository, sessions())
        runCurrent()
        viewModel.open(ProfileSection.Privacy)
        runCurrent()
        assertTrue(repository.consentWrites.isEmpty())
        repository.consentGate = CompletableDeferred()
        repository.consentResult = AppResult.Failure(AppError(ErrorKind.Server))
        viewModel.changeConsent("PERFILADO", true)
        viewModel.changeConsent("COMERCIAL", false)
        runCurrent()
        assertEquals(listOf("PERFILADO" to true), repository.consentWrites)
        assertFalse(viewModel.state.value.consents?.current?.get("PERFILADO")?.granted ?: true)
        repository.consentGate?.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.saved)
        assertEquals(ErrorKind.Server, viewModel.state.value.error?.kind)
        repository.consentResult = AppResult.Success(Unit)
        viewModel.changeConsent("PERFILADO", true)
        runCurrent()
        assertTrue(viewModel.state.value.consents?.current?.get("PERFILADO")?.granted == true)
        assertTrue(viewModel.state.value.consents?.current?.get("COMERCIAL")?.granted == true)
    }

    @Test fun nativeLanguageCallbackRunsOnlyAfterAConfirmedUpdate() = runTest {
        val repository = Repository()
        val viewModel = ProfileViewModel(repository, sessions())
        val applied = mutableListOf<String>()
        runCurrent()
        viewModel.open(ProfileSection.Language)
        runCurrent()
        assertTrue(repository.languageWrites.isEmpty())
        viewModel.languageChanged("en-US")
        repository.languageResult = AppResult.Failure(AppError(ErrorKind.Server))
        viewModel.saveLanguage { applied += it }
        runCurrent()
        assertTrue(applied.isEmpty())
        assertEquals("es-CO", viewModel.state.value.preferences?.preferredLanguage)
        repository.languageResult = AppResult.Success(
            ProfilePreferences("en-US", "es-CO", "CORREO")
        )
        viewModel.saveLanguage { applied += it }
        runCurrent()
        assertEquals(listOf("en-US"), applied)
        assertEquals("es-CO", viewModel.state.value.preferences?.mailLanguage)
        assertTrue(viewModel.state.value.saved)
    }

    @Test fun interestsStartAsAnUnconfirmedReplacementAndDoNotWriteOnSelection() = runTest {
        val repository = Repository()
        val viewModel = ProfileViewModel(repository, sessions())
        runCurrent()
        viewModel.open(ProfileSection.Interests)
        runCurrent()
        assertFalse(viewModel.state.value.interestsSaved)
        assertTrue(viewModel.state.value.selectedInterests.isEmpty())
        viewModel.toggleInterest("unknown")
        assertTrue(viewModel.state.value.selectedInterests.isEmpty())
        viewModel.toggleInterest("cultura")
        assertTrue(repository.interestsWrites.isEmpty())
        viewModel.saveInterests()
        runCurrent()
        assertEquals(listOf(listOf("cultura")), repository.interestsWrites)
        assertTrue(viewModel.state.value.interestsSaved)
    }

    @Test fun cancellationRightsClearTheLocalSessionOnlyAfterTheReceipt() = runTest {
        val repository = Repository()
        val sessions = sessions()
        val viewModel = ProfileViewModel(repository, sessions)
        runCurrent()
        viewModel.open(ProfileSection.Rights)
        runCurrent()
        assertTrue(repository.rightsWrites.isEmpty())
        assertTrue(sessions.state.value is SessionState.Authenticated)
        viewModel.submitRights(RightsRequest(RightsType.CANCELACION))
        runCurrent()
        assertEquals(RightsType.CANCELACION, repository.rightsWrites.single().type)
        assertTrue(sessions.state.value is SessionState.Anonymous)
        assertNull(viewModel.state.value.profile)
        assertNull(viewModel.state.value.receipt)
    }

    @Test fun logoutDiscardsEvenACancellationResistantLateProfileResponse() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply {
            profileRead = {
                withContext(NonCancellable) { gate.await() }
                AppResult.Success(MemberProfile("previous", "Private previous name", null))
            }
        }
        val sessions = sessions()
        val viewModel = ProfileViewModel(repository, sessions)
        runCurrent()
        sessions.clear()
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.state.value.profile)
        assertEquals("", viewModel.state.value.name)
        assertFalse(viewModel.state.value.saved)
    }

    @Test fun immediateReloginForTheSameUserReloadsWithoutOldPrivateState() = runTest {
        val repository = Repository()
        val sessions = sessions()
        val viewModel = ProfileViewModel(repository, sessions)
        runCurrent()
        assertEquals("Original", viewModel.state.value.name)
        val gate = CompletableDeferred<Unit>()
        repository.profileRead = {
            gate.await()
            AppResult.Success(MemberProfile("member", "New session name", null))
        }
        sessions.clear()
        check(
            sessions.install(
                TokenPair(testToken(), "new-refresh"),
                sessions.snapshot().generation
            )
        )
        runCurrent()
        assertNull(viewModel.state.value.profile)
        assertEquals("", viewModel.state.value.name)
        gate.complete(Unit)
        runCurrent()
        assertEquals("New session name", viewModel.state.value.name)
    }

    @Test fun retryAfterBlocksEditsFromBypassingTheSubmissionDelay() = runTest {
        val repository = Repository().apply {
            nameResult = AppResult.Failure(AppError(ErrorKind.RateLimited, retryAfterSeconds = 2))
        }
        val viewModel = ProfileViewModel(repository, sessions())
        runCurrent()
        viewModel.nameChanged("Nuevo")
        viewModel.saveName()
        runCurrent()
        assertEquals(2L, viewModel.state.value.retrySeconds)
        viewModel.nameChanged("Otro nombre")
        viewModel.saveName()
        runCurrent()
        assertEquals(1, repository.names.size)
        advanceTimeBy(2000)
        runCurrent()
        repository.nameResult = AppResult.Success(Unit)
        viewModel.saveName()
        runCurrent()
        assertEquals(2, repository.names.size)
        assertTrue(viewModel.state.value.saved)
    }

    @Test fun privatePdfCallbackIsNotCalledForARejectedExport() = runTest {
        val repository = Repository()
        val viewModel = ProfileViewModel(repository, sessions())
        runCurrent()
        val delivered = mutableListOf<ByteArray>()
        viewModel.exportData { delivered += it }
        runCurrent()
        assertTrue(delivered.isEmpty())
        repository.exportResult = AppResult.Success("%PDF-1.4".encodeToByteArray())
        viewModel.exportData { delivered += it }
        runCurrent()
        assertEquals(1, delivered.size)
    }

    @Test fun cancelledOldLoadCannotHideTheProgressOfItsReplacement() = runTest {
        val oldGate = CompletableDeferred<Unit>()
        val newGate = CompletableDeferred<Unit>()
        var calls = 0
        val repository = Repository().apply {
            profileRead = {
                calls++
                if (calls == 1) {
                    withContext(NonCancellable) { oldGate.await() }
                } else {
                    newGate.await()
                }
                AppResult.Success(MemberProfile("member", "Newest", null))
            }
        }
        val viewModel = ProfileViewModel(repository, sessions())
        runCurrent()
        viewModel.reload()
        runCurrent()
        oldGate.complete(Unit)
        runCurrent()
        assertTrue(viewModel.state.value.loading)
        assertNull(viewModel.state.value.profile)
        newGate.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.loading)
        assertEquals("Newest", viewModel.state.value.name)
    }

    @Test fun rightsDetailRetryUsesTheOriginalReferenceAndMakesAnotherRead() = runTest {
        val repository = Repository().apply { caseFailure = AppError(ErrorKind.Offline) }
        val viewModel = ProfileViewModel(repository, sessions())
        runCurrent()
        viewModel.openCase("request-1")
        runCurrent()
        assertEquals(ErrorKind.Offline, viewModel.state.value.error?.kind)
        assertNull(viewModel.state.value.caseDetail)
        repository.caseFailure = null
        viewModel.reload()
        runCurrent()
        assertEquals(2, repository.caseReads)
        assertEquals("request-1", viewModel.state.value.caseDetail?.reference)
        assertNull(viewModel.state.value.error)
    }
}
