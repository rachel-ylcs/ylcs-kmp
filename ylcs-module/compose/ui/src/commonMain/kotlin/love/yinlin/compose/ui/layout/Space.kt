package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme

@Suppress("UnusedReceiverParameter")
@Composable
@NonRestartableComposable
fun RowScope.Space(size: Dp = Theme.padding.h) {
    Layout(modifier = Modifier.width(size), measurePolicy = MeasurePolicies.Space)
}

@Suppress("UnusedReceiverParameter")
@Composable
@NonRestartableComposable
fun ColumnScope.Space(size: Dp = Theme.padding.v) {
    Layout(modifier = Modifier.height(size), measurePolicy = MeasurePolicies.Space)
}