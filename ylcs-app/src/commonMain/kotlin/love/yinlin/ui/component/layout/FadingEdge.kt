package love.yinlin.ui.component.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpRect
import love.yinlin.common.Colors

fun Modifier.fadingEdges(
    edges: DpRect,
    startAlpha: Float = 1f,
    endAlpha: Float = 0f
): Modifier = composed {
    val edgesPx = with(LocalDensity.current) { edges.toRect() }
    graphicsLayer { alpha = 0.99f }.drawWithCache {
        val colorsT2B = listOf(Colors.Black.copy(alpha = endAlpha), Colors.Black.copy(alpha = startAlpha))
        val colorsB2T = colorsT2B.reversed()
        val width = size.width
        val height = size.height
        val startBrush =  Brush.horizontalGradient(colors = colorsT2B, startX = 0f, endX = edgesPx.left)
        val endBrush = Brush.horizontalGradient(colors = colorsB2T, startX = width - edgesPx.right, endX = width)
        val topBrush = Brush.verticalGradient(colors = colorsT2B, startY = 0f, endY = edgesPx.top)
        val bottomBrush = Brush.verticalGradient(colors = colorsB2T, startY = height - edgesPx.bottom, endY = height)
        onDrawWithContent {
            drawContent()
            if (edgesPx.left > 0) drawRect(startBrush, Offset(0f, 0f), Size(edgesPx.left, height), blendMode = BlendMode.DstIn)
            if (edgesPx.right > 0) drawRect(endBrush, Offset(width - edgesPx.right, 0f), Size(edgesPx.right, height), blendMode = BlendMode.DstIn)
            if (edgesPx.top > 0) drawRect(brush = topBrush, topLeft = Offset(0f, 0f), size = Size(width, edgesPx.top), blendMode = BlendMode.DstIn)
            if (edgesPx.bottom > 0) drawRect(bottomBrush, Offset(0f, height - edgesPx.bottom), Size(width, edgesPx.bottom), blendMode = BlendMode.DstIn)
        }
    }
}