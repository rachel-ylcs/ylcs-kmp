package love.yinlin.ui.component.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs

@Composable
fun OffsetLayout(
	x: Dp = 0.dp,
	y: Dp = 0.dp,
	zIndex: Float = 2f,
	content: @Composable () -> Unit
) {
	Box(
		modifier = Modifier.layout { measurable, constraints ->
			val placeable = measurable.measure(constraints)
			layout(
				width = (placeable.width - abs(x.roundToPx())).coerceAtLeast(0),
				height = (placeable.height - abs(y.roundToPx())).coerceAtLeast(0)
			) {
				placeable.placeRelative(x.roundToPx(), y.roundToPx())
			}
		}.zIndex(zIndex)
	) {
		content()
	}
}