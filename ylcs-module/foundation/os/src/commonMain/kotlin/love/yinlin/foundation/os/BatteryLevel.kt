package love.yinlin.foundation.os

import androidx.compose.runtime.Stable
import kotlin.math.roundToInt

@Stable
sealed interface BatteryLevel {
    /** 电量百分比，取值范围为 0 到 100。 */
    data class Value(val percent: Int) : BatteryLevel {
        init {
            require(percent in 0 .. 100)
        }
    }

    /** 当前平台未提供电量查询能力。 */
    data object Unsupported : BatteryLevel

    /** 系统明确报告设备没有电池。 */
    data object NoBattery : BatteryLevel

    /** 查询失败或系统未提供有效电量。 */
    data object Unknown : BatteryLevel

    companion object {
        internal fun fromPercentage(percent: Int): BatteryLevel =
            if (percent in 0 .. 100) Value(percent) else Unknown

        internal fun fromFraction(fraction: Double): BatteryLevel =
            if (fraction in 0.0 .. 1.0) Value((fraction * 100).roundToInt()) else Unknown

        internal fun fromWindows(flag: Int, percent: Int): BatteryLevel = when {
            flag != 255 && (flag and 128) != 0 -> NoBattery
            else -> fromPercentage(percent)
        }
    }
}
