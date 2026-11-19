package co.vitasmarta.mobile.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.ErrorKind
import kotlinx.coroutines.delay

@Composable
fun ErrorNotice(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    useErrorColor: Boolean = true
) {
    var remaining by remember(error) {
        mutableLongStateOf(
            error.retryAfterSeconds?.coerceAtLeast(0) ?: 0
        )
    }
    LaunchedEffect(error) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }
    val message = when (error.kind) {
        ErrorKind.Offline -> R.string.error_offline
        ErrorKind.Timeout -> R.string.error_timeout
        ErrorKind.Unauthorized -> R.string.error_credentials
        ErrorKind.Forbidden -> R.string.error_forbidden
        ErrorKind.Locked -> R.string.error_locked
        ErrorKind.RateLimited -> R.string.error_rate
        ErrorKind.AdditionalVerification -> R.string.error_additional
        ErrorKind.Validation -> R.string.error_validation
        ErrorKind.Conflict -> R.string.error_conflict
        ErrorKind.NotFound -> R.string.error_not_found
        ErrorKind.InvalidResponse -> R.string.error_invalid_response
        ErrorKind.Configuration -> R.string.error_configuration
        else -> R.string.service_unavailable_body
    }
    Column(
        modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(message),
            color = if (useErrorColor) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (onRetry !=
            null
        ) {
            Button(
                shape = MaterialTheme.shapes.small,
                onClick = onRetry,
                enabled = remaining == 0L
            ) {
                Text(
                    if (remaining ==
                        0L
                    ) {
                        stringResource(R.string.retry)
                    } else {
                        stringResource(R.string.retry_after, remaining)
                    }
                )
            }
        }
    }
}
