package love.yinlin.compose.animation

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.*
import kotlin.math.*

@Stable
interface Interpolator {
    @Suppress("NOTHING_TO_INLINE")
    companion object {
        const val PI = 3.141592f

        // 加速插值
        // 先慢后快
        // 0 ~ 1 -> 0 ~ 1
        inline fun accelerate(x: Float): Float = x * x

        // 减速插值
        // 先快后慢
        inline fun decelerate(x: Float): Float = 1 - (1 - x) * (1 - x)

        // 加减速插值
        // 两边慢 中间快
        // 0 ~ 1 -> 0 ~ 1
        inline fun accelerateDecelerate(x: Float): Float = (cos((x + 1) * PI) + 1) / 2

        // 秋千插值
        // 先后再加速向前
        // 0 ~ 1 -> 0 ~ -0.13 ~ 1
        inline fun anticipate(x: Float): Float = x * x * (3 * x - 2)

        // 振荡插值
        // 快速临界后振荡
        // 0 ~ 1 -> 0 ~ 1 ~ 0.7 ~ 1 ~ 0.9 ~ 1
        inline fun bounce(x: Float): Float {
            val t = x * 1.226f
            val y = when {
                t < 0.3535f -> packFloats(t, 0f)
                t < 0.7408f -> packFloats(t - 0.54719f, 0.7f)
                t < 0.9644f -> packFloats(t - 0.8526f, 0.9f)
                else -> packFloats(t - 1.0435f, 0.95f)
            }
            val a = unpackFloat1(y)
            val b = unpackFloat2(y)
            return a * a * 8 + b
        }

        // 回弹插值
        // 快速临界后回落
        // 0 ~ 1 -> 0 ~ 1.13 ~ 1
        inline fun overshoot(x: Float): Float {
            val t = x - 1
            return t * t * (3 * t + 2) + 1
        }

        // 正弦插值
        // 0 ~ 1 -> 0 ~ 1 ~ 0 ~ -1 ~ 0
        inline fun cycle(x: Float): Float = sin(2 * PI * x)
    }
}