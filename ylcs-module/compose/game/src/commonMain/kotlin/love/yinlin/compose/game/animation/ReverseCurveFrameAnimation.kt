package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable

@Stable
class ReverseCurveFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false,
) : FrameAnimation(totalFrame, isInfinite) {
    override fun calcProgress(t: Int, f: Int): Float {
        val s = t / 3
        //         { 1 - ((x - s) / s) ^ 2  , 0 <= x <= s
        // f(x) =  { 1                      , s <= x <= 2 * s
        //         { ((x - 3 * s) / s) ^ 2  , 2 * s <= x <= 3 * s
        return when (f) {
            in 0 .. s -> 1 - (f - s) * (f - s) / (s * s).toFloat()
            in 2 * s .. 3 * s -> (f - 3 * s) * (f - 3 * s) / (s * s).toFloat()
            else -> 1f
        }
    }
}