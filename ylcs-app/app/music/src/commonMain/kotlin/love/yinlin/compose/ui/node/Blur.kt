package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import love.yinlin.compose.Colors

typealias BlurState = HazeState

fun Modifier.blurSource(state: BlurState): Modifier = this.hazeSource(state)

private val DefaultBlurStyle = HazeStyle(
    blurRadius = 10.dp,
    backgroundColor = Colors(0xFF292929),
    tint = HazeTint(Colors(0x8C292929))
)

@OptIn(ExperimentalHazeApi::class)
fun Modifier.blurTarget(state: BlurState): Modifier = this.hazeEffect(
    state = state,
    style = DefaultBlurStyle
) {
    inputScale = HazeInputScale.Fixed(0.66667f)
}