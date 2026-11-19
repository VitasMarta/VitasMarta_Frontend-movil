package co.vitasmarta.mobile.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.vitasmarta.mobile.R

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)
internal val LightColors = lightColorScheme(
    primary = Color(0xFF1D2B5C), onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E1DE), onPrimaryContainer = Color(0xFF1D2B5C),
    secondary = Color(0xFF006B3F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2E7), onSecondaryContainer = Color(0xFF00452A),
    background = Color(0xFFFDFBFA), onBackground = Color(0xFF1D2B5C),
    surface = Color.White, onSurface = Color(0xFF1D2B5C),
    surfaceVariant = Color(0xFFEDF1F0), onSurfaceVariant = Color(0xFF526174),
    surfaceContainer = Color(0xFFF3F5F4), surfaceContainerHigh = Color(0xFFE6EFE9),
    surfaceContainerLow = Color(0xFFF8F8F6),
    outline = Color(0xFF6E7B84), outlineVariant = Color(0xFFD8DFE2),
    error = Color(0xFFB91C1C), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)
internal val DarkColors = darkColorScheme(
    primary = Color(0xFFF4E1DE), onPrimary = Color(0xFF1D2B5C),
    primaryContainer = Color(0xFF263A56), onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Color(0xFF79DBAC), onSecondary = Color(0xFF003D28),
    secondaryContainer = Color(0xFF154531), onSecondaryContainer = Color(0xFFB1F2D1),
    background = Color(0xFF0D1723), onBackground = Color(0xFFF2F4F7),
    surface = Color(0xFF111F2E), onSurface = Color(0xFFF2F4F7),
    surfaceVariant = Color(0xFF203747), onSurfaceVariant = Color(0xFFBDC8D2),
    surfaceContainer = Color(0xFF172B3B), surfaceContainerHigh = Color(0xFF203747),
    surfaceContainerLow = Color(0xFF10202C),
    outline = Color(0xFF8295A5), outlineVariant = Color(0xFF344858),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF5D2023), onErrorContainer = Color(0xFFFFDAD6)
)
private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Poppins, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight)
private val Type = Typography(
    headlineLarge = type(30, 38, FontWeight.Bold),
    headlineMedium = type(26, 34, FontWeight.SemiBold),
    titleLarge = type(22, 28, FontWeight.SemiBold),
    titleMedium = type(18, 26, FontWeight.SemiBold),
    bodyLarge = type(16, 24), bodyMedium = type(14, 22), bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold),
    labelMedium = type(12, 18, FontWeight.Medium), labelSmall = type(11, 16, FontWeight.Medium)
)

@Composable
fun VitasMartaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Type,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
