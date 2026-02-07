package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme

@Composable
@NonRestartableComposable
fun VerticalDivider(
    thickness: Dp = Theme.border.v7,
    color: Color = Theme.color.outline,
    modifier: Modifier = Modifier,
) {
    Layout(modifier = Modifier.fillMaxHeight().width(thickness).then(modifier).drawBehind {
        val thicknessPx = thickness.toPx()
        drawLine(
            color = color,
            strokeWidth = thicknessPx,
            start = Offset(thicknessPx / 2, 0f),
            end = Offset(thicknessPx / 2, size.height),
        )
    }, measurePolicy = MeasurePolicies.Space)
}

@Suppress("UnusedReceiverParameter")
@Composable
@NonRestartableComposable
fun RowScope.Divider(
    thickness: Dp = Theme.border.v7,
    color: Color = Theme.color.outline,
    modifier: Modifier = Modifier,
) {
    VerticalDivider(thickness, color, modifier)
}

@Composable
@NonRestartableComposable
fun HorizontalDivider(
    thickness: Dp = Theme.border.v7,
    color: Color = Theme.color.outline,
    modifier: Modifier = Modifier,
) {
    Layout(modifier = Modifier.fillMaxWidth().height(thickness).then(modifier).drawBehind {
        val thicknessPx = thickness.toPx()
        drawLine(
            color = color,
            strokeWidth = thicknessPx,
            start = Offset(0f, thicknessPx / 2),
            end = Offset(size.width, thicknessPx / 2),
        )
    }, measurePolicy = MeasurePolicies.Space)
}

@Suppress("UnusedReceiverParameter")
@Composable
@NonRestartableComposable
fun ColumnScope.Divider(
    thickness: Dp = Theme.border.v7,
    color: Color = Theme.color.outline,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(thickness, color, modifier)
}