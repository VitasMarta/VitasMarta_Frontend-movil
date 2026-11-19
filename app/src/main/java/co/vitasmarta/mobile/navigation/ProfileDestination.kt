package co.vitasmarta.mobile.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.preferences.AppLanguage
import co.vitasmarta.mobile.feature.profile.presentation.ProfileRoute
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileDestination(onLogout: () -> Unit) {
    val context = LocalContext.current
    PdfDocumentHost("VitasMarta-datos.pdf") { save ->
        ProfileRoute(
            onLogout = onLogout,
            onLanguageChanged = { language ->
                AppLanguage.apply(context, language)
                if (Build.VERSION.SDK_INT < 33) context.activity()?.recreate()
            },
            onExport = save
        )
    }
}

/** A document picker grants access to the single file chosen by the user. */
@Composable
fun PdfDocumentHost(fileName: String, content: @Composable ((ByteArray) -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val transfer: PdfTransferViewModel = hiltViewModel()
    val exportError = stringResource(R.string.export_save_error)
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = transfer.consume()
        if (uri != null && bytes == null) {
            scope.launch { snackbar.showSnackbar(exportError) }
        } else if (uri != null && bytes != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    try {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: throw IOException("Document unavailable")
                        stream.use { it.write(bytes) }
                        true
                    } catch (_: IOException) {
                        false
                    } catch (_: SecurityException) {
                        false
                    }
                }
                if (!saved) snackbar.showSnackbar(exportError)
            }
        }
    }
    Box {
        content { bytes ->
            if (transfer.offer(bytes)) {
                picker.launch(fileName)
            } else {
                scope.launch { snackbar.showSnackbar(exportError) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
