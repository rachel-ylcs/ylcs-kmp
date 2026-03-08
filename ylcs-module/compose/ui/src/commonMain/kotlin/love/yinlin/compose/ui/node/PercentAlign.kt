package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import love.yinlin.compose.platform.inspector

private class PercentAlignNode(
    var xPercent: () -> Float,
    var yPercent: () -> Float
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val containerWidth = constraints.maxWidth
        val containerHeight = constraints.maxHeight
        return layout(placeable.width, placeable.height) {
            val x = ((containerWidth - placeable.width) * xPercent()).toInt()
            val y = ((containerHeight - placeable.height) * yPercent()).toInt()
            placeable.placeRelative(x, y)
        }
    }
}

private data class PercentAlignElement(
    val xPercent: () -> Float,
    val yPercent: () -> Float
) : ModifierNodeElement<PercentAlignNode>() {
    override fun create(): PercentAlignNode = PercentAlignNode(xPercent, yPercent)
    override fun update(node: PercentAlignNode) {
        node.xPercent = xPercent
        node.yPercent = yPercent
        node.invalidateMeasurement()
    }

    override fun InspectorInfo.inspectableProperties() = inspector("percentAlign") {
        "xPercent" bind xPercent()
        "yPercent" bind yPercent()
    }
}

val AlignStart: () -> Float = { 0f }
val AlignCenter: () -> Float = { 0.5f }
val AlignEnd: () -> Float = { 1f }

@Stable
fun Modifier.align(xPercent: () -> Float = AlignStart, yPercent: () -> Float = AlignStart): Modifier =
    this then PercentAlignElement(xPercent, yPercent)