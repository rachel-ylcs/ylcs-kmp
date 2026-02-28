package love.yinlin.compose.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import love.yinlin.compose.Theme

@Composable
fun LinearProgress(
    value: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Theme.color.backgroundVariant,
    activeColor: Color = Theme.color.primary,
    shape: Shape = Theme.shape.circle
) {
    val minWidth = Theme.size.input5
    val minHeight = Theme.size.box3

    Layout(modifier = modifier.clip(shape = shape).drawWithCache {
        onDrawWithContent {
            drawRect(color = trackColor)
            drawRect(color = activeColor, size = Size(width = size.width * value.coerceIn(0f, 1f), height = size.height))
        }
    }) { _, constraints ->
        val width = if (constraints.hasFixedWidth) constraints.maxWidth else minWidth.toPx().toInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = if (constraints.hasFixedHeight) constraints.maxHeight else minHeight.toPx().toInt().coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) { }
    }
}