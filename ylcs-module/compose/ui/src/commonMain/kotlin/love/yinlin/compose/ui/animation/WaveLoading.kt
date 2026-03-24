package love.yinlin.compose.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.*
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
    private fun InfiniteTransition.animateValue(duration: Int, index: Int): State<Float> {
        return animateFloat(0.3f, 1f, infiniteRepeatable(
            animation = tween(duration, index * duration / 3, LinearEasing),
            repeatMode = RepeatMode.Reverse
        ))
    }

    @Composable
    override fun Content(color: Color, modifier: Modifier) {
        val minSize = Theme.size.icon
        val transition = rememberInfiniteTransition()
        val duration = Theme.animation.duration.v4
        val value1 by transition.animateValue(duration, 0)
        val value2 by transition.animateValue(duration, 1)
        val value3 by transition.animateValue(duration, 2)

        Layout(modifier = modifier.defaultMinSize(minSize, minSize).drawBehind {
            drawItem(0, value1, color)
            drawItem(1, value2, color)
            drawItem(2, value3, color)
        }, measurePolicy = MeasurePolicies.Empty)
    }
}