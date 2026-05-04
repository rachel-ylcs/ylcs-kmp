package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.*

@Stable
class BlurState {
    internal val hazeState = HazeState()

    companion object {
        internal val Blur = HazeBlurStyle(
            blurRadius = 10.dp,
            backgroundColor = Color(0xFF292929),
            colorEffect = HazeColorEffect.tint(Color(0x8C292929))
        )
        internal val Acrylic = HazeBlurStyle(
            blurRadius = 10.dp,
            backgroundColor = Color(0xDD292929),
            colorEffect = HazeColorEffect.tint(Color(0x6C292929))
        )
    }
}

fun Modifier.blurSource(state: BlurState): Modifier = this.hazeSource(state.hazeState)

fun Modifier.blurTarget(state: BlurState): Modifier = this.hazeEffect(state = state.hazeState) {
    blurEffect {
        style = BlurState.Blur
        inputScale = HazeInputScale.Fixed(0.66667f)
    }
}

fun Modifier.acrylicTarget(state: BlurState): Modifier = this.hazeEffect(state = state.hazeState) {
    blurEffect {
        style = BlurState.Acrylic
        inputScale = HazeInputScale.Fixed(0.66667f)
    }
}