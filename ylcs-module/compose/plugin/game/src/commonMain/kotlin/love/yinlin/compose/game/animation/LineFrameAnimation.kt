package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable

@Stable
class LineFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false,
    adapter: FrameAdapter? = null,
) : FrameAnimation(totalFrame, isInfinite, adapter) {
    override fun calcProgress(t: Int, f: Int): Float = (f + 1f) / t
}