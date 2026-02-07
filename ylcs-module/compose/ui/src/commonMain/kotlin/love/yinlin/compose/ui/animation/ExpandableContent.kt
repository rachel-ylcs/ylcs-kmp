package love.yinlin.compose.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import love.yinlin.compose.Theme

@Composable
fun ExpandableContent(
    isExpanded: Boolean,
    duration: Int? = null,
    content: @Composable () -> Unit
) {
    val animationDuration = duration ?: Theme.animation.duration.v9
    val easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = tween(
            durationMillis = animationDuration,
            easing = easing
        )),
        exit = shrinkVertically(animationSpec = tween(
            durationMillis = animationDuration,
            easing = easing
        ))
    ) {
        content()
    }
}