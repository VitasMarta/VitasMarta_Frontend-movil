package co.vitasmarta.mobile.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.auth.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Survives Activity recreation, but never saves the private PDF to SavedState or disk. */
@HiltViewModel
class PdfTransferViewModel @Inject constructor(private val sessions: SessionStore) : ViewModel() {
    private var pending: ByteArray? = null
    private var generation: Long? = null

    init {
        viewModelScope.launch {
            sessions.state.collect { state ->
                if (state is SessionState.Anonymous ||
                    generation != sessions.snapshot().generation
                ) {
                    clear()
                }
            }
        }
    }

    fun offer(bytes: ByteArray): Boolean {
        val snapshot = sessions.snapshot()
        if (snapshot.tokens == null) return false
        pending = bytes
        generation = snapshot.generation
        return true
    }

    fun consume(): ByteArray? {
        val snapshot = sessions.snapshot()
        val bytes = pending.takeIf { snapshot.tokens != null && snapshot.generation == generation }
        clear()
        return bytes
    }

    fun discard() = clear()

    private fun clear() {
        pending = null
        generation = null
    }
}
