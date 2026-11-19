package co.vitasmarta.mobile.core.auth

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

fun testToken(
    subject: String = "12345678-1234-1234-1234-123456789012",
    extra: String = ""
): String {
    val payload = """
        {"iss":"svc-identidad","sub":"$subject",
        "rol":"TURISTA","exp":4102444800$extra}
    """.trimIndent()
    return "test." +
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            payload.toByteArray(StandardCharsets.UTF_8)
        ) +
        ".test"
}

class SessionStoreTest {
    @Test fun invalidClaimsCannotCreateASession() {
        val store = SessionStore()
        for (token in listOf(
            "broken",
            "test.e30.test",
            testToken("invalid"),
            testToken(extra = ",\"exp\":0")
        )) {
            assertFalse(store.install(TokenPair(token, "refresh"), store.snapshot().generation))
            assertNull(store.snapshot().tokens)
        }
    }

    @Test fun anOldResponseCannotRestoreSessionAfterLogout() {
        val store = SessionStore()
        val generation = store.snapshot().generation
        store.clear()
        assertFalse(store.install(TokenPair(testToken(), "refresh"), generation))
        assertTrue(store.state.value is SessionState.Anonymous)
    }

    @Test fun tokensAreRedactedWhenLoggedAccidentally() {
        val pair = TokenPair(testToken(), "very-private-refresh")
        assertFalse(pair.toString().contains("very-private-refresh"))
        assertFalse(SessionSnapshot(0, pair).toString().contains(pair.accessToken))
    }

    @Test fun aNewLoginForTheSameUserStillChangesTheObservableSession() {
        val store = SessionStore()
        assertTrue(store.install(TokenPair(testToken(), "first"), store.snapshot().generation))
        val first = store.state.value as SessionState.Authenticated
        store.clear()
        assertTrue(store.install(TokenPair(testToken(), "second"), store.snapshot().generation))
        val second = store.state.value as SessionState.Authenticated
        assertEquals(first.userId, second.userId)
        assertNotEquals(first, second)
        assertEquals(store.snapshot().generation, second.generation)
        // Token rotation inside a session does not masquerade as a change of account.
        store.install(TokenPair(testToken(), "third"), store.snapshot().generation)
        assertEquals(second, store.state.value)
    }
}
