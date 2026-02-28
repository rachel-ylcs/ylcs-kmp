package love.yinlin.compose.ui.node

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

fun Modifier.dashBorder(
    width: Dp,
    color: Color,
    shape: Shape = RectangleShape
): Modifier = composed {
    val density = LocalDensity.current
    val stroke = remember(width, density) {
        val strokeWidthPx = with(density) { width.toPx() }
        val dashGap = strokeWidthPx * 2
        val dashWidth = dashGap * 2

        Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(dashWidth, dashGap), phase = 0f)
        )
    }

    drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        if (outline is Outline.Rounded) {
            drawRoundRect(
                color = color,
                topLeft = outline.bounds.topLeft,
                size = outline.bounds.size,
                cornerRadius = outline.roundRect.topLeftCornerRadius,
                style = stroke
            )
        }
        else drawRect(color = color, style = stroke)
    }
}