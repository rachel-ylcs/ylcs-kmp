package love.yinlin.compose.game.animation

class EaseOutFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false,
    adapter: FrameAdapter? = null,
) : FrameAnimation(totalFrame, isInfinite, adapter) {
    override fun calcProgress(t: Int, f: Int): Float = ((t + 1f) / f).let { 1 - (1 - it) * (1 - it) }
}