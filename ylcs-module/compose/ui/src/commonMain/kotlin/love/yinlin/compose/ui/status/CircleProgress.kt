package love.yinlin.compose.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastCoerceIn
import love.yinlin.compose.Theme

@Composable
fun CircleProgress(
    value: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Theme.color.backgroundVariant,
    activeColor: Color = Theme.color.primary
) {
    val minRadius = Theme.size.input7

    Layout(modifier = modifier.drawWithContent {
        val minEdge = minOf(size.width, size.height)
        val radius = minEdge / 2
        val actualStrokeWidth = radius / 4

        val center = Offset(size.width / 2, size.height / 2)
        val arcRadius = radius - (actualStrokeWidth / 2)
        val arcSize = Size(arcRadius * 2, arcRadius * 2)
        val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)

        drawCircle(
            color = trackColor,
            radius = arcRadius,
            center = center,
            style = Stroke(width = actualStrokeWidth)
        )

        drawArc(
            color = activeColor,
            startAngle = -90f,
            sweepAngle = value.fastCoerceIn(0f, 1f) * 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = actualStrokeWidth, cap = StrokeCap.Round)
        )
    }) { _, constraints ->
        val minRadiusPx = minRadius.toPx().toInt()
        val width = if (constraints.hasFixedWidth) constraints.maxWidth else minRadiusPx.fastCoerceIn(constraints.minWidth, constraints.maxWidth)
        val height = if (constraints.hasFixedHeight) constraints.maxHeight else minRadiusPx.fastCoerceIn(constraints.minHeight, constraints.maxHeight)
        val radius = minOf(width, height)

        layout(radius, radius) {}
    }
}