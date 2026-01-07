package love.yinlin.compose.game.animation

class EaseInFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false,
    adapter: FrameAdapter? = null,
) : FrameAnimation(totalFrame, isInfinite, adapter) {
    override fun calcProgress(t: Int, f: Int): Float = ((t + 1f) / f).let { it * it }
}