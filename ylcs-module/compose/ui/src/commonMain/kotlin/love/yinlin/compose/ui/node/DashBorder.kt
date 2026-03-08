package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.platform.inspector

private class DashBorderNode(
    var width: Dp,
    var color: Color,
    var shape: Shape
) : Modifier.Node(), DrawModifierNode {
    private var cachedStroke: Stroke? = null

    override fun ContentDrawScope.draw() {
        drawContent()

        val strokeWidthPx = width.toPx()
        val stroke = cachedStroke ?: Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(strokeWidthPx * 4, strokeWidthPx * 2), phase = 0f)
        ).also { cachedStroke = it }
        when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rounded -> {
                drawRoundRect(
                    color = color,
                    topLeft = outline.bounds.topLeft,
                    size = outline.bounds.size,
                    cornerRadius = outline.roundRect.topLeftCornerRadius,
                    style = stroke
                )
            }
            is Outline.Generic -> drawPath(path = outline.path, color = color, style = stroke)
            else -> drawRect(color = color, style = stroke)
        }
    }

    fun update(width: Dp, color: Color, shape: Shape) {
        this.width = width
        this.color = color
        this.shape = shape
        invalidateDraw()
    }
}

private data class DashBorderElement(
    val width: Dp,
    val color: Color,
    val shape: Shape
) : ModifierNodeElement<DashBorderNode>() {
    override fun create(): DashBorderNode = DashBorderNode(width, color, shape)
    override fun update(node: DashBorderNode) = node.update(width, color, shape)
    override fun InspectorInfo.inspectableProperties() = inspector("DashBorder") {
        "width" bind width
        "color" bind color
        "shape" bind shape
    }
}

@Stable
fun Modifier.dashBorder(width: Dp, color: Color, shape: Shape = RectangleShape): Modifier =
    this then DashBorderElement(width, color, shape)