package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable

@Stable
class LineFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false,
) : FrameAnimation(totalFrame, isInfinite) {
    override fun calcProgress(t: Int, f: Int): Float = (f + 1f) / t
}