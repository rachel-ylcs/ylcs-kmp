package love.yinlin.compose.ui.node

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.LayoutDirection
import love.yinlin.compose.Colors

private class FadingEdgeNode(
    var startAlpha: Float,
    var endAlpha: Float,
    var padding: PaddingValues
) : Modifier.Node(), DrawModifierNode {
    var startBrush: Brush? = null
    var endBrush: Brush? = null
    var topBrush: Brush? = null
    var bottomBrush: Brush? = null
    var lastSize: Size? = null
    var left: Float = 0f
    var top: Float = 0f
    var right: Float = 0f
    var bottom: Float = 0f

    override fun ContentDrawScope.draw() {
        if (size != lastSize) {
            val colorsT2B = listOf(Colors.Black.copy(alpha = endAlpha), Colors.Black.copy(alpha = startAlpha))
            val colorsB2T = colorsT2B.reversed()
            val width = size.width
            val height = size.height

            left = padding.calculateLeftPadding(LayoutDirection.Ltr).toPx()
            top = padding.calculateTopPadding().toPx()
            right = padding.calculateRightPadding(LayoutDirection.Ltr).toPx()
            bottom = padding.calculateBottomPadding().toPx()

            startBrush = if (left > 0f) Brush.horizontalGradient(colors = colorsT2B, startX = 0f, endX = left) else null
            endBrush = if (right > 0f) Brush.horizontalGradient(colors = colorsB2T, startX = width - right, endX = width) else null
            topBrush = if (top > 0f) Brush.verticalGradient(colors = colorsT2B, startY = 0f, endY = top) else null
            bottomBrush = if (bottom > 0f) Brush.verticalGradient(colors = colorsB2T, startY = height - bottom, endY = height) else null

            lastSize = size
        }

        drawContent()

        startBrush?.let { drawRect(it, Offset(0f, 0f), Size(left, size.height), blendMode = BlendMode.DstIn) }
        endBrush?.let { drawRect(it, Offset(size.width - right, 0f), Size(right, size.height), blendMode = BlendMode.DstIn) }
        topBrush?.let { drawRect(it, Offset(0f, 0f), Size(size.width, top), blendMode = BlendMode.DstIn) }
        bottomBrush?.let { drawRect(it, Offset(0f, size.height - bottom), Size(size.width, bottom), blendMode = BlendMode.DstIn) }
    }
}

private data class FadingEdgeElement(
    val startAlpha: Float,
    val endAlpha: Float,
    val padding: PaddingValues
) : ModifierNodeElement<FadingEdgeNode>() {
    override fun create(): FadingEdgeNode = FadingEdgeNode(startAlpha, endAlpha, padding)
    override fun update(node: FadingEdgeNode) {
        node.startAlpha = startAlpha
        node.endAlpha = endAlpha
        node.padding = padding
        node.lastSize = null
        node.invalidateDraw()
    }
}

fun Modifier.fadingEdge(startAlpha: Float = 1f, endAlpha: Float = 0f, padding: PaddingValues): Modifier =
    this.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.then(FadingEdgeElement(startAlpha, endAlpha, padding))