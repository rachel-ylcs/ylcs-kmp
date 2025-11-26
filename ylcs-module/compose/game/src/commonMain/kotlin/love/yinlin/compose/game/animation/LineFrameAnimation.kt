package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

@Stable
class LineFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false
) : FrameAnimation(totalFrame, isInfinite) {
    override val progress by derivedStateOf { ((frame + 1f) / total).coerceIn(0f, 1f) }
}