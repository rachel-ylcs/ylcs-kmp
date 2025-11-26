package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

@Stable
class CurveFrameAnimation(
    totalFrame: Int,
    isInfinite: Boolean = false
) : FrameAnimation(totalFrame, isInfinite) {
    override val progress by derivedStateOf {
        val fpa = total / 3
        //         { 1 - ((x - fpa) / fpa) ^ 2  , 0       <= x <= fpa
        // f(x) =  { 1                          , fpa     <= x <= 2 * fpa
        //         { ((x - 3 * fpa) / fpa) ^ 2  , 2 * fpa <= x <= 3 * fpa
        when (val f = frame) {
            in 0 .. fpa -> 1 - (f - fpa) * (f - fpa) / (fpa * fpa).toFloat()
            in 2 * fpa .. 3 * fpa -> (f - 3 * fpa) * (f - 3 * fpa) / (fpa * fpa).toFloat()
            else -> 1f
        }.coerceIn(0f, 1f)
    }
}