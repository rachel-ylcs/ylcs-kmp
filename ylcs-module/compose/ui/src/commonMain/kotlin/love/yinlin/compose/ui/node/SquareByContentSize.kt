package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import love.yinlin.compose.platform.inspector

private class SquareByContentSizeNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val desiredSide = maxOf(placeable.width, placeable.height)
        val minSide = maxOf(constraints.minWidth, constraints.minHeight)
        val maxSide = minOf(constraints.maxWidth, constraints.maxHeight)

        val side = if (maxSide >= minSide) desiredSide.coerceIn(minimumValue = minSide, maximumValue = maxSide) else maxSide

        return layout(width = side, height = side) {
            placeable.placeRelative(
                x = (side - placeable.width) / 2,
                y = (side - placeable.height) / 2
            )
        }
    }
}

private data object SquareByContentSizeElement : ModifierNodeElement<SquareByContentSizeNode>() {
    override fun create(): SquareByContentSizeNode = SquareByContentSizeNode()
    override fun update(node: SquareByContentSizeNode) = Unit
    override fun InspectorInfo.inspectableProperties() = inspector("SquareByContentSize")
}

@Stable
fun Modifier.squareByContentSize(): Modifier = this then SquareByContentSizeElement