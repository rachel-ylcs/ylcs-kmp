package love.yinlin.compose.ui.node

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import love.yinlin.compose.Colors

fun Modifier.fadingEdge(
    startAlpha: Float = 1f,
    endAlpha: Float = 0f,
    paddingProvider: (DpSize) -> PaddingValues
): Modifier = this.graphicsLayer { alpha = 0.99f }.drawWithCache {
    val colorsT2B = listOf(Colors.Black.copy(alpha = endAlpha), Colors.Black.copy(alpha = startAlpha))
    val colorsB2T = colorsT2B.reversed()
    val width = size.width
    val height = size.height
    val padding = paddingProvider(size.toDpSize())
    val left = padding.calculateLeftPadding(LayoutDirection.Ltr).toPx()
    val top = padding.calculateTopPadding().toPx()
    val right = padding.calculateRightPadding(LayoutDirection.Ltr).toPx()
    val bottom = padding.calculateBottomPadding().toPx()
    val startBrush =  Brush.horizontalGradient(colors = colorsT2B, startX = 0f, endX = left)
    val endBrush = Brush.horizontalGradient(colors = colorsB2T, startX = width - right, endX = width)
    val topBrush = Brush.verticalGradient(colors = colorsT2B, startY = 0f, endY = top)
    val bottomBrush = Brush.verticalGradient(colors = colorsB2T, startY = height - bottom, endY = height)
    onDrawWithContent {
        drawContent()
        if (left > 0) drawRect(startBrush, Offset(0f, 0f), Size(left, height), blendMode = BlendMode.DstIn)
        if (right > 0) drawRect(endBrush, Offset(width - right, 0f), Size(right, height), blendMode = BlendMode.DstIn)
        if (top > 0) drawRect(brush = topBrush, topLeft = Offset(0f, 0f), size = Size(width, top), blendMode = BlendMode.DstIn)
        if (bottom > 0) drawRect(bottomBrush, Offset(0f, height - bottom), Size(width, bottom), blendMode = BlendMode.DstIn)
    }
}