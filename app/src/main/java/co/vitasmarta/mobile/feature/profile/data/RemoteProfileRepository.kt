package co.vitasmarta.mobile.feature.profile.data

import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.core.network.AuthenticatedApiExecutor
import co.vitasmarta.mobile.feature.profile.domain.ConsentEvidence
import co.vitasmarta.mobile.feature.profile.domain.InterestCategory
import co.vitasmarta.mobile.feature.profile.domain.MemberProfile
import co.vitasmarta.mobile.feature.profile.domain.ProfileConsents
import co.vitasmarta.mobile.feature.profile.domain.ProfilePreferences
import co.vitasmarta.mobile.feature.profile.domain.ProfileRepository
import co.vitasmarta.mobile.feature.profile.domain.RightsCase
import co.vitasmarta.mobile.feature.profile.domain.RightsReceipt
import co.vitasmarta.mobile.feature.profile.domain.RightsRequest
import co.vitasmarta.mobile.feature.profile.domain.RightsStep
import co.vitasmarta.mobile.feature.profile.domain.RightsType
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class RemoteProfileRepository(
    private val api: ProfileApi?,
    private val authenticated: AuthenticatedApiExecutor,
    private val publicCategories: ProfileCategoriesApi?,
    private val executor: ApiExecutor
) : ProfileRepository {
    private suspend fun <T : Any> privateCall(
        block: suspend (ProfileApi, String, String) -> Response<T>
    ): AppResult<T> {
        val remote = api ?: return failure(ErrorKind.Configuration)
        return authenticated.execute { bearer, correlation -> block(remote, bearer, correlation) }
    }
    private fun failure(kind: ErrorKind) = AppResult.Failure(AppError(kind))
    private inline fun <A, B> AppResult<A>.map(transform: (A) -> B): AppResult<B> = when (this) {
        is AppResult.Failure -> this
        is AppResult.Success -> AppResult.Success(transform(value))
    }
    private fun PreferencesDto.domain() = ProfilePreferences(language, mailLanguage, channel)
    private fun RightsCaseDto.domain() = RightsCase(
        reference,
        tipo,
        estado,
        receivedAt,
        pasos.map { RightsStep(it.paso, it.at, it.resultado) }
    )
    override suspend fun profile(): AppResult<MemberProfile> =
        privateCall { remote, bearer, id -> remote.profile(bearer, id) }.map {
            MemberProfile(it.userId, it.visibleName, it.updatedAt)
        }
    override suspend fun updateName(name: String): AppResult<Unit> =
        privateCall { remote, bearer, id ->
            remote.updateName(bearer, id, NameUpdate(name))
        }.map { Unit }
    override suspend fun preferences(): AppResult<ProfilePreferences> =
        privateCall { remote, bearer, id -> remote.preferences(bearer, id) }.map { it.domain() }
    override suspend fun updateLanguage(language: String): AppResult<ProfilePreferences> {
        if (language !in setOf("es-CO", "en-US")) return failure(ErrorKind.Validation)
        return privateCall { remote, bearer, id ->
            remote.updateLanguage(bearer, id, LanguageUpdate(language))
        }.map { it.domain() }
    }
    override suspend fun consents(): AppResult<ProfileConsents> =
        privateCall { remote, bearer, id -> remote.consents(bearer, id) }.map { response ->
            ProfileConsents(
                response.vigente.mapValues { (_, consent) ->
                    consent?.let { ConsentEvidence(it.otorgado, it.version, it.at) }
                }
            )
        }
    override suspend fun setConsent(purpose: String, granted: Boolean): AppResult<Unit> {
        if (purpose !in setOf("COMERCIAL", "PERFILADO")) return failure(ErrorKind.Validation)
        return privateCall { remote, bearer, id ->
            remote.setConsent(bearer, id, purpose, ConsentUpdate(granted))
        }.map { Unit }
    }
    override suspend fun categories(): AppResult<List<InterestCategory>> {
        val remote = publicCategories ?: return failure(ErrorKind.Configuration)
        return executor.execute { remote.categories() }.map { values ->
            values.map { InterestCategory(it.slug, it.nombre) }
        }
    }
    override suspend fun replaceInterests(slugs: List<String>): AppResult<Unit> {
        if (slugs.size > 60 || slugs.any { it.isBlank() || it.length > 60 }) {
            return failure(ErrorKind.Validation)
        }
        return privateCall { remote, bearer, id ->
            remote.replaceInterests(bearer, id, InterestsUpdate(slugs.distinct()))
        }.map { Unit }
    }
    override suspend fun rightsCases(): AppResult<List<RightsCase>> =
        privateCall { remote, bearer, id -> remote.rightsCases(bearer, id) }
            .map { values -> values.map { it.domain() } }
    override suspend fun rightsCase(reference: String): AppResult<RightsCase> =
        privateCall { remote, bearer, id -> remote.rightsCase(bearer, id, reference) }
            .map { it.domain() }
    override suspend fun submitRights(request: RightsRequest): AppResult<RightsReceipt> {
        val detail = when (request.type) {
            RightsType.RECTIFICACION -> {
                if (request.value.isBlank()) return failure(ErrorKind.Validation)
                mapOf("campo" to "nombre_visible", "valor_nuevo" to request.value)
            }
            RightsType.OPOSICION -> {
                if (request.value !in
                    setOf("COMERCIAL", "PERFILADO")
                ) {
                    return failure(ErrorKind.Validation)
                }
                mapOf("finalidad" to request.value)
            }
            else -> emptyMap()
        }
        var created = false
        return when (
            val result = privateCall { remote, bearer, id ->
                remote.submitRights(bearer, id, RightsSubmission(request.type.name, detail))
                    .also { created = it.code() == 201 }
            }
        ) {
            is AppResult.Failure -> result
            is AppResult.Success -> if (created && result.value.reference.isNotBlank()) {
                AppResult.Success(RightsReceipt(result.value.reference))
            } else {
                failure(ErrorKind.InvalidResponse)
            }
        }
    }
    override suspend fun exportData(): AppResult<ByteArray> {
        var valid = false
        val result = privateCall { remote, bearer, id ->
            val response = remote.exportData(bearer, id)
            if (!response.isSuccessful) {
                Response.error<ByteArray>(
                    response.code(),
                    response.errorBody() ?: "".toResponseBody()
                )
            } else {
                val bytes = withContext(Dispatchers.IO) {
                    response.body()?.use { body ->
                        val type = body.contentType()
                        if (type?.type != "application" ||
                            type.subtype != "pdf" ||
                            body.contentLength() > MAX_PDF_BYTES
                        ) {
                            ByteArray(0)
                        } else {
                            val out = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            body.byteStream().use { input ->
                                while (out.size() <= MAX_PDF_BYTES) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    out.write(buffer, 0, read)
                                }
                            }
                            out.toByteArray()
                        }
                    } ?: ByteArray(0)
                }
                valid = response.code() == 200 &&
                    bytes.size in 5..MAX_PDF_BYTES &&
                    bytes.copyOfRange(0, 5).decodeToString() == "%PDF-"
                Response.success(bytes, response.headers())
            }
        }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> if (valid) result else failure(ErrorKind.InvalidResponse)
        }
    }

    private companion object {
        const val MAX_PDF_BYTES = 10 * 1024 * 1024
    }
}
