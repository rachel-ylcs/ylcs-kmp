package love.yinlin.compose.game.animation

class EaseOutFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false
) : FrameAnimation(totalFrame, isInfinite) {
    override fun calcProgress(t: Int, f: Int): Float = ((t + 1f) / f).let { 1 - (1 - it) * (1 - it) }
}