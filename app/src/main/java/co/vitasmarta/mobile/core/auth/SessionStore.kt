package co.vitasmarta.mobile.core.auth

import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

class TokenPair(val accessToken: String, val refreshToken: String) {
    override fun toString(): String = "TokenPair(redacted)"
}

class SessionSnapshot(val generation: Long, val tokens: TokenPair?) {
    override fun toString(): String = "SessionSnapshot(generation=$generation, redacted)"
}

sealed interface SessionState {
    data object Anonymous : SessionState
    data class Authenticated(val userId: String, val generation: Long = 0) : SessionState
}

/** Claims only drive UI. M1 and the owning service perform authorization. */
@Singleton
class SessionStore @Inject constructor() {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Anonymous)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()
    private var generation = 0L
    private var tokens: TokenPair? = null

    @Synchronized
    fun snapshot(): SessionSnapshot = SessionSnapshot(generation, tokens)

    @Synchronized
    fun clear(): Long {
        generation++
        tokens = null
        mutableState.value = SessionState.Anonymous
        return generation
    }

    @Synchronized
    fun clearIfCurrent(expected: Long) {
        if (generation == expected) clear()
    }

    @Synchronized
    fun install(pair: TokenPair, expected: Long, clock: Clock = Clock.systemUTC()): Boolean {
        if (generation != expected) return false
        val userId = touristId(pair.accessToken, clock) ?: return false
        if (pair.refreshToken.isBlank()) return false
        tokens = pair
        mutableState.value = SessionState.Authenticated(userId, generation)
        return true
    }

    private fun touristId(token: String, clock: Clock): String? {
        return try {
            val parts = token.split('.')
            if (parts.size != 3) return null
            val payload =
                Json.parseToJsonElement(
                    String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                ) as? JsonObject
                    ?: return null
            fun claim(key: String) = (payload[key] as? JsonPrimitive)?.contentOrNull
            val expiry = (payload["exp"] as? JsonPrimitive)?.longOrNull ?: return null
            if (claim("iss") != "svc-identidad" ||
                claim("rol") != "TURISTA" ||
                expiry <= clock.instant().epochSecond
            ) {
                return null
            }
            UUID.fromString(claim("sub") ?: return null).toString()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
