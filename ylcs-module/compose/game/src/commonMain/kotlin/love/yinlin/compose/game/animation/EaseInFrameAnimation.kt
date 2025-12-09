package love.yinlin.compose.game.animation

class EaseInFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false
) : FrameAnimation(totalFrame, isInfinite) {
    override fun calcProgress(t: Int, f: Int): Float = ((t + 1f) / f).let { it * it }
}