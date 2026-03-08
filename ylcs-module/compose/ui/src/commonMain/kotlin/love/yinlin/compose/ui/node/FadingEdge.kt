package love.yinlin.compose.ui.node

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import love.yinlin.compose.Colors
import love.yinlin.compose.platform.inspector

private class FadingEdgeNode(
    var startAlpha: Float,
    var endAlpha: Float,
    var padding: PaddingValues
) : Modifier.Node(), DrawModifierNode {
    val paint = Paint()
    var startBrush: Brush? = null
    var endBrush: Brush? = null
    var topBrush: Brush? = null
    var bottomBrush: Brush? = null
    var lastSize: Size? = null
    var left: Float = 0f
    var top: Float = 0f
    var right: Float = 0f
    var bottom: Float = 0f

    private fun ContentDrawScope.updateBrushesIfNeeded() {
        if (size == lastSize) return

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

    override fun ContentDrawScope.draw() {
        drawIntoCanvas { canvas ->
            val width = size.width
            val height = size.height
            canvas.saveLayer(Rect(0f, 0f, width, height), paint)
            drawContent()
            updateBrushesIfNeeded()
            startBrush?.let { drawRect(it, Offset(0f, 0f), Size(left, height), blendMode = BlendMode.DstIn) }
            endBrush?.let { drawRect(it, Offset(width - right, 0f), Size(right, height), blendMode = BlendMode.DstIn) }
            topBrush?.let { drawRect(it, Offset(0f, 0f), Size(width, top), blendMode = BlendMode.DstIn) }
            bottomBrush?.let { drawRect(it, Offset(0f, height - bottom), Size(width, bottom), blendMode = BlendMode.DstIn) }
            canvas.restore()
        }
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
    override fun InspectorInfo.inspectableProperties() = inspector("FadingEdge") {
        "startAlpha" bind startAlpha
        "endAlpha" bind endAlpha
        "padding" bind padding
    }
}

@Stable
fun Modifier.fadingEdge(startAlpha: Float = 1f, endAlpha: Float = 0f, padding: PaddingValues): Modifier =
    this then FadingEdgeElement(startAlpha, endAlpha, padding)