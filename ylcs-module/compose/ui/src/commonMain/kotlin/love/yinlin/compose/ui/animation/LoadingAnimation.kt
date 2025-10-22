package love.yinlin.compose.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.LocalAnimationSpeed

@Composable
fun LoadingAnimation(
    size: Dp = CustomTheme.size.icon,
    color: Color = MaterialTheme.colorScheme.primary,
    duration: Int = LocalAnimationSpeed.current,
    num: Int = 3,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()
    val values = Array(num) {
        transition.animateFloat(0.3f, 1f, infiniteRepeatable(
            animation = tween(duration, it * duration / num, LinearEasing),
            repeatMode = RepeatMode.Reverse
        ))
    }

    Canvas(modifier = modifier.padding(CustomTheme.padding.innerIconSpace).size(size)) {
        values.forEachIndexed { index, state ->
            val width = (size / 5).toPx()
            val spacing = (this.size.width - (num * width)) / 2
            scale(scaleX = 1f, scaleY = state.value) {
                drawLine(
                    color = color.copy(alpha = state.value),
                    start = Offset(width / 2 + (width + spacing) * index, 0f),
                    end = Offset(width / 2 + (width + spacing) * index, this.size.height),
                    strokeWidth = width
                )
            }
        }
    }
}