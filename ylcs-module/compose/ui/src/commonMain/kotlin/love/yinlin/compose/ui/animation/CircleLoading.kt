package love.yinlin.compose.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.layout.MeasurePolicies

@Stable
object CircleLoading : IndeterminateLoadingAnimation {
    @Composable
    override fun Content(color: Color, modifier: Modifier) {
        val minSize = Theme.size.icon
        val duration = Theme.animation.duration.v4

        val length by rememberInfiniteTransition().animateFloat(0f, 180f, infiniteRepeatable(
            animation = tween(easing = LinearEasing, durationMillis = duration * 2),
            repeatMode = RepeatMode.Reverse
        ))

        val progress by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(
            animation = tween(easing = LinearEasing, durationMillis = duration)
        ))

        Layout(Modifier.defaultMinSize(minSize, minSize).then(modifier).drawBehind {
            val width = this.size.width
            drawArc(
                color = color,
                startAngle = progress - length - 90f,
                sweepAngle = length,
                useCenter = false,
                topLeft = Offset(width * 0.05f, width * 0.05f),
                size = Size(width * 0.9f, width * 0.9f),
                style = Stroke(
                    width = width * 0.1f,
                    cap = StrokeCap.Round
                )
            )
        }, measurePolicy = MeasurePolicies.Empty)
    }
}