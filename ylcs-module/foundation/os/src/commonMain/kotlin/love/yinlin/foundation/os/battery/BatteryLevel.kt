package love.yinlin.foundation.os.battery

import androidx.annotation.IntRange
import kotlin.math.roundToInt

/**
 * 电量
 */
sealed interface BatteryLevel {
    @get:IntRange(0, 100) val percent: Int

    /**
     * 已知电量
     */
    data class Value(@IntRange(0, 100) override val percent: Int) : BatteryLevel

    /**
     * 不支持
     */
    data object Unsupported : BatteryLevel {
        override val percent: Int = 0
    }

    /**
     * 无电池
     */
    data object NoBattery : BatteryLevel {
        override val percent: Int = 0
    }

    /**
     * 未知
     */
    data object Unknown : BatteryLevel {
        override val percent: Int = 0
    }

    companion object {
        internal fun fromPercentage(percent: Int): BatteryLevel = if (percent in 0 .. 100) Value(percent) else Unknown
        internal fun fromFraction(fraction: Double): BatteryLevel = if (fraction in 0.0 .. 1.0) Value((fraction * 100).roundToInt()) else Unknown
        internal fun fromWindows(flag: Int, percent: Int): BatteryLevel = if (flag != 255 && (flag and 128) != 0) NoBattery else fromPercentage(percent)
    }
}
