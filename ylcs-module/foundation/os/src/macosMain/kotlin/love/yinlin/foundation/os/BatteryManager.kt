package love.yinlin.foundation.os

import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext
import love.yinlin.foundation.os.internal.RachelBatteryLevel

@OptIn(ExperimentalForeignApi::class)
actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.io {
        when (val percent = RachelBatteryLevel()) {
            -2 -> BatteryLevel.NoBattery
            else -> BatteryLevel.fromPercentage(percent)
        }
    }
}
