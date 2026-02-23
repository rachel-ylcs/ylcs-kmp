package love.yinlin.compose.ui.container

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import love.yinlin.compose.extension.rememberState

@Composable
fun AdaptiveTwoBox(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit
) {
    var targetOffset1 by rememberState { IntOffset.Zero }
    var targetOffset2 by rememberState { IntOffset.Zero }
    val animatedOffset1 by animateIntOffsetAsState(targetOffset1)
    val animatedOffset2 by animateIntOffsetAsState(targetOffset2)

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }

        val w1 = placeables[0].width
        val h1 = placeables[0].height
        val w2 = placeables[1].width
        val h2 = placeables[1].height

        val canFitHorizontal = (w1 + w2) <= constraints.maxWidth
        val canFitVertical = (h1 + h2) <= constraints.maxHeight
        val isHorizontal = if (canFitHorizontal && !canFitVertical) true
        else if (!canFitHorizontal && canFitVertical) false
        else canFitHorizontal

        val layoutWidth: Int
        val layoutHeight: Int
        val outX = IntArray(2)
        val outY = IntArray(2)

        if (isHorizontal) {
            layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else (w1 + w2)
            layoutHeight = maxOf(h1, h2).coerceIn(constraints.minHeight, constraints.maxHeight)
            with(horizontalArrangement) {
                arrange(layoutWidth, intArrayOf(w1, w2), layoutDirection, outX)
            }
            outY[0] = verticalAlignment.align(h1, layoutHeight)
            outY[1] = verticalAlignment.align(h2, layoutHeight)
        } else {
            layoutWidth = maxOf(w1, w2).coerceIn(constraints.minWidth, constraints.maxWidth)
            layoutHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else (h1 + h2)
            with(verticalArrangement) {
                arrange(layoutHeight, intArrayOf(h1, h2), outY)
            }
            outX[0] = horizontalAlignment.align(w1, layoutWidth, layoutDirection)
            outX[1] = horizontalAlignment.align(w2, layoutWidth, layoutDirection)
        }

        val newOffset1 = IntOffset(outX[0], outY[0])
        val newOffset2 = IntOffset(outX[1], outY[1])
        if (newOffset1 != targetOffset1) targetOffset1 = newOffset1
        if (newOffset2 != targetOffset2) targetOffset2 = newOffset2

        layout(layoutWidth, layoutHeight) {
            placeables[0].placeRelative(animatedOffset1)
            placeables[1].placeRelative(animatedOffset2)
        }
    }
}