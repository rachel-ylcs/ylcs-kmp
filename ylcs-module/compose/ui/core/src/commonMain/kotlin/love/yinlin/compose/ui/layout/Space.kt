package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.ui.CustomTheme

@Stable
private object SpaceMeasurePolicy : MeasurePolicy {
	override fun MeasureScope.measure(
		measurables: List<Measurable>,
		constraints: Constraints
	): MeasureResult {
		return with(constraints) {
			val width = if (hasFixedWidth) maxWidth else 0
			val height = if (hasFixedHeight) maxHeight else 0
			layout(width, height) {}
		}
	}
}

@Composable
@NonRestartableComposable
fun RowScope.Space(size: Dp = CustomTheme.padding.horizontalSpace) {
	Layout(Modifier.width(size), SpaceMeasurePolicy)
}

@Composable
@NonRestartableComposable
fun ColumnScope.Space(size: Dp = CustomTheme.padding.verticalSpace) {
	Layout(Modifier.height(size), SpaceMeasurePolicy)
}