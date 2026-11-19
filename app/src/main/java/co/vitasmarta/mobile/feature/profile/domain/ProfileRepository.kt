package co.vitasmarta.mobile.feature.profile.domain

import co.vitasmarta.mobile.core.common.AppResult

class MemberProfile(val userId: String, val visibleName: String?, val updatedAt: String?) {
    override fun toString() = "MemberProfile(redacted)"
}

data class ProfilePreferences(
    val preferredLanguage: String,
    val mailLanguage: String,
    val channel: String
)
data class ConsentEvidence(val granted: Boolean, val version: String?, val at: String?)
data class ProfileConsents(val current: Map<String, ConsentEvidence?>)
data class InterestCategory(val slug: String, val names: Map<String, String>) {
    fun displayName(locale: String) = names[locale] ?: names[locale.substringBefore('-')]
        ?: names["es"] ?: names["es-CO"] ?: names["en"] ?: slug
}

enum class RightsType { ACCESO, RECTIFICACION, OPOSICION, CANCELACION }
class RightsRequest(val type: RightsType, val value: String = "") {
    override fun toString() = "RightsRequest(type=$type, redacted)"
}
class RightsReceipt(val reference: String) {
    override fun toString() = "RightsReceipt(redacted)"
}
class RightsCase(
    val reference: String,
    val type: String,
    val status: String,
    val receivedAt: String?,
    val steps: List<RightsStep>
) {
    override fun toString() = "RightsCase(redacted)"
}
data class RightsStep(val name: String, val at: String?, val result: String?)

interface ProfileRepository {
    suspend fun profile(): AppResult<MemberProfile>
    suspend fun updateName(name: String): AppResult<Unit>
    suspend fun preferences(): AppResult<ProfilePreferences>
    suspend fun updateLanguage(language: String): AppResult<ProfilePreferences>
    suspend fun consents(): AppResult<ProfileConsents>
    suspend fun setConsent(purpose: String, granted: Boolean): AppResult<Unit>
    suspend fun categories(): AppResult<List<InterestCategory>>
    suspend fun replaceInterests(slugs: List<String>): AppResult<Unit>
    suspend fun rightsCases(): AppResult<List<RightsCase>>
    suspend fun rightsCase(reference: String): AppResult<RightsCase>
    suspend fun submitRights(request: RightsRequest): AppResult<RightsReceipt>
    suspend fun exportData(): AppResult<ByteArray>
}
