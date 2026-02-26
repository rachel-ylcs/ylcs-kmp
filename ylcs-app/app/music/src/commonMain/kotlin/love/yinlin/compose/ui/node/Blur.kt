package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import love.yinlin.compose.Colors

typealias BlurState = HazeState

internal fun Modifier.blurSource(state: BlurState): Modifier = this.hazeSource(state)

private val DefaultBlurStyle = HazeStyle(
    blurRadius = 10.dp,
    backgroundColor = Colors(0xFF292929),
    tint = HazeTint(Colors(0x8C292929))
)

@OptIn(ExperimentalHazeApi::class)
internal fun Modifier.blurTarget(state: BlurState): Modifier = this.hazeEffect(
    state = state,
    style = DefaultBlurStyle
) {
    inputScale = HazeInputScale.Fixed(0.66667f)
}