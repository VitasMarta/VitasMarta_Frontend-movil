package co.vitasmarta.mobile.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** Decorative, code-native reference to the Sierra and the Caribbean; not a destination photo. */
@Composable
fun TerritoryMark(modifier: Modifier = Modifier) {
    val mountain = MaterialTheme.colorScheme.secondary
    val sea = MaterialTheme.colorScheme.primary
    val sky = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier) {
        drawRoundRect(sky, cornerRadius = CornerRadius(size.width * .24f), size = size)
        val ridge = Path().apply {
            moveTo(size.width * .13f, size.height * .62f)
            lineTo(size.width * .44f, size.height * .24f)
            lineTo(size.width * .59f, size.height * .44f)
            lineTo(size.width * .70f, size.height * .31f)
            lineTo(size.width * .92f, size.height * .62f)
        }
        drawPath(ridge, mountain, style = Stroke(width = size.width * .045f))
        for (level in listOf(.71f, .82f)) {
            val wave = Path().apply {
                moveTo(size.width * .13f, size.height * level)
                cubicTo(
                    size.width * .35f,
                    size.height * (level - .12f),
                    size.width * .59f,
                    size.height * (level + .12f),
                    size.width * .87f,
                    size.height * level
                )
            }
            drawPath(wave, sea, style = Stroke(width = size.width * .035f))
        }
        drawCircle(
            mountain,
            radius = size.width * .045f,
            center = Offset(size.width * .76f, size.height * .19f)
        )
    }
}
