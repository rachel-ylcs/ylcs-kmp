package love.yinlin.foundation.os

import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.io {
        LinuxBatteryReader.readLevel()
    }
}
