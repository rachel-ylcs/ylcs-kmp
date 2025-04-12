package love.yinlin.ui.component.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable

@Composable
fun ExpandableLayout(
	isExpanded: Boolean,
	duration: Int = 300,
	content: @Composable () -> Unit
) {
	AnimatedVisibility(
		visible = isExpanded,
		enter = expandVertically(animationSpec = tween(
			durationMillis = duration,
			easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
		)),
		exit = shrinkVertically(animationSpec = tween(
			durationMillis = duration,
			easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
		))
	) {
		content()
	}
}