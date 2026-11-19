package co.vitasmarta.mobile.navigation

import co.vitasmarta.mobile.MainDispatcherRule
import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenPair
import co.vitasmarta.mobile.core.auth.testToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PdfTransferViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun documentRemainsInTheViewModelUntilPickerReturnsAndIsConsumedOnlyOnce() = runTest {
        val sessions = SessionStore()
        sessions.install(TokenPair(testToken(), "test-refresh"), 0)
        val transfer = PdfTransferViewModel(sessions)
        runCurrent()
        val bytes = "%PDF-test".toByteArray()
        assertTrue(transfer.offer(bytes))
        assertArrayEquals(bytes, transfer.consume())
        assertNull(transfer.consume())
    }

    @Test fun documentCannotCrossAnImmediateReloginForTheSameUser() = runTest {
        val sessions = SessionStore()
        sessions.install(TokenPair(testToken(), "first"), 0)
        val transfer = PdfTransferViewModel(sessions)
        runCurrent()
        transfer.offer("%PDF-private".toByteArray())
        sessions.clear()
        sessions.install(TokenPair(testToken(), "second"), sessions.snapshot().generation)
        assertNull(transfer.consume())
        runCurrent()
    }
}
