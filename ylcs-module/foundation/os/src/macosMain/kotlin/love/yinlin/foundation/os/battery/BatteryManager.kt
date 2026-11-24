package love.yinlin.foundation.os.battery

import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.io {
        MacBatteryApi.readLevel()
    }
}
