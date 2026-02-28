package love.yinlin.compose.ui.animation

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.Layout
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.layout.MeasurePolicies

@Stable
object WaveLoading : IndeterminateLoadingAnimation {
    private fun DrawScope.drawItem(index: Int, value: Float, color: Color) {
        val width = size.width
        val w = width * 0.2f
        val spacing = (width - (3 * w)) / 2
        scale(scaleX = 1f, scaleY = value) {
            drawLine(
                color = color.copy(alpha = value),
                start = Offset(w / 2 + (w + spacing) * index, 0f),
                end = Offset(w / 2 + (w + spacing) * index, size.height),
                strokeWidth = w
            )
        }
    }

    @Composable
    fun InfiniteTransition.animateValue(index: Int): State<Float> {
        val duration = Theme.animation.duration.v4
        return animateFloat(0.3f, 1f, infiniteRepeatable(
            animation = tween(duration, index * duration / 3, LinearEasing),
            repeatMode = RepeatMode.Reverse
        ))
    }

    @Composable
    override fun Content(color: Color, modifier: Modifier) {
        val minSize = Theme.size.icon
        val transition = rememberInfiniteTransition()
        val value1 by transition.animateValue(0)
        val value2 by transition.animateValue(1)
        val value3 by transition.animateValue(2)

        Layout(modifier = Modifier.defaultMinSize(minSize, minSize).then(modifier).drawBehind {
            drawItem(0, value1, color)
            drawItem(1, value2, color)
            drawItem(2, value3, color)
        }, measurePolicy = MeasurePolicies.Empty)
    }
}