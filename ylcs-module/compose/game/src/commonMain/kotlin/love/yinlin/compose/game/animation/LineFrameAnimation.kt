package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable

@Stable
class LineFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false,
    totalStep: Int = 1,
) : FrameAnimation(totalFrame, isInfinite, totalStep) {
    override fun calcProgress(t: Int, f: Int): Float = (f + 1f) / t
}