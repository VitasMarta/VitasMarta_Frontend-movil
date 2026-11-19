package co.vitasmarta.mobile.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePaletteTest {
    @Test fun lightPaletteHasReadableTextAndControlBoundaries() {
        verify(LightColors)
    }

    @Test fun darkPaletteHasReadableTextAndControlBoundaries() {
        verify(DarkColors)
    }

    private fun verify(colors: ColorScheme) {
        val textPairs = listOf(
            colors.onBackground to colors.background,
            colors.onSurface to colors.surface,
            colors.onSurfaceVariant to colors.surfaceContainerHigh,
            colors.onPrimary to colors.primary,
            colors.onPrimaryContainer to colors.primaryContainer,
            colors.onSecondary to colors.secondary,
            colors.onSecondaryContainer to colors.secondaryContainer,
            colors.error to colors.background,
            colors.onErrorContainer to colors.errorContainer
        )
        textPairs.forEach { (text, surface) ->
            assertTrue("Text contrast must be >= 4.5:1", contrast(text, surface) >= 4.5)
        }
        assertTrue(
            "Input boundary contrast must be >= 3:1",
            contrast(colors.outline, colors.surface) >= 3
        )
        assertTrue(
            "Input boundary contrast must be >= 3:1",
            contrast(colors.outline, colors.background) >= 3
        )
    }

    private fun contrast(a: Color, b: Color): Double {
        val light = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (light + .05) / (dark + .05)
    }
}
