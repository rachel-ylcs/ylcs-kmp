package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect

typealias BlurState = HazeState

fun Modifier.blurSource(state: BlurState): Modifier = this.hazeSource(state)

private val DefaultBlurStyle = HazeBlurStyle(
    blurRadius = 10.dp,
    backgroundColor = Color(0xFF292929),
    colorEffect = HazeColorEffect.tint(Color(0x8C292929))
)

fun Modifier.blurTarget(state: BlurState): Modifier = this.hazeEffect(state = state) {
    blurEffect {
        style = DefaultBlurStyle
        inputScale = HazeInputScale.Fixed(0.66667f)
    }
}