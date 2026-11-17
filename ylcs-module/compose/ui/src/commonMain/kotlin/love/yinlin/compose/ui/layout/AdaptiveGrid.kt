package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme

@Composable
fun AdaptiveGrid(
    minCellWidth: Dp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Theme.padding.h,
    verticalPadding: Dp = Theme.padding.v,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Grid(
        modifier = modifier,
        config = {
            if (constraints.hasBoundedWidth) {
                val availableWidth = constraints.maxWidth.toDp()
                val columns = ((availableWidth + horizontalPadding) / (minCellWidth + horizontalPadding)).toInt().coerceAtLeast(1)
                val cellWidth = (availableWidth - horizontalPadding * (columns - 1).toFloat()) / columns.toFloat()
                repeat(columns) {
                    column(cellWidth)
                }
            }
            else column(minCellWidth)
            gap(verticalPadding, horizontalPadding)
        }
    ) {
        content()
    }
}