package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import love.yinlin.compose.Colors
import love.yinlin.compose.platform.inspector

private class OuterShadowNode(
    var shape: Shape,
    var radius: Dp
) : DrawModifierNode, Modifier.Node(), ObserverModifierNode {
    private var shadowPainter: DropShadowPainter? = null

    val obtainPainter: DropShadowPainter get() = shadowPainter ?: requireGraphicsContext().shadowContext.createDropShadowPainter(shape, Shadow(
        radius = radius, color = Colors.Dark.copy(alpha = 0.75f), offset = DpOffset(radius, radius)
    )).also { shadowPainter = it }

    fun update(shape: Shape, radius: Dp) {
        val painterInvalidated = this.shape != shape || this.radius != radius
        if (painterInvalidated) shadowPainter = null
        this.shape = shape
        this.radius = radius
    }

    override fun ContentDrawScope.draw() {
        with(obtainPainter) { draw(size) }
        drawContent()
    }

    override fun onObservedReadsChanged() {
        shadowPainter = null
        invalidateDraw()
    }
}

private data class OuterShadowElement(
    val shape: Shape,
    val radius: Dp
) : ModifierNodeElement<OuterShadowNode>() {
    override fun create(): OuterShadowNode = OuterShadowNode(shape, radius)
    override fun update(node: OuterShadowNode) = node.update(shape, radius)
    override fun InspectorInfo.inspectableProperties() = inspector("OuterShadow") {
        "shape" bind shape
        "radius" bind radius
    }
}

@Stable
fun Modifier.shadow(shape: Shape, radius: Dp): Modifier =
    this then OuterShadowElement(shape, radius)