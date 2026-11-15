package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import love.yinlin.compose.Theme

@Composable
fun MasonryLayout(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Theme.padding.h,
    verticalSpacing: Dp = Theme.padding.v,
    content: @Composable () -> Unit
) {
    val defaultWidth = Theme.size.cell1

    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val hasKnownWidth = constraints.hasBoundedWidth && constraints.minWidth == constraints.maxWidth
        val width = constraints.constrainWidth(if (hasKnownWidth) constraints.maxWidth else defaultWidth.roundToPx())

        val hGap = horizontalSpacing.roundToPx().coerceAtLeast(0).coerceAtMost(width)
        val vGap = verticalSpacing.roundToPx().coerceAtLeast(0)
        val itemWidth = (width - hGap) / 2

        val heights: IntArray = [0, 0]
        val placements: MutableList<Triple<Placeable, Int, Int>> = []

        measurables.forEach { measurable ->
            val column = if (heights[0] <= heights[1]) 0 else 1
            val placeable = measurable.measure(Constraints.fixedWidth(itemWidth))

            val x = column * (itemWidth + hGap)
            val y = heights[column]

            placements += Triple(placeable, x, y)
            heights[column] += placeable.height + vGap
        }

        val totalHeight = if (placements.isEmpty()) 0 else (heights.maxOrNull()!! - vGap).coerceAtLeast(0)

        layout(width, constraints.constrainHeight(totalHeight)) {
            placements.forEach { [placeable, x, y] ->
                placeable.placeRelative(x, y)
            }
        }
    }
}