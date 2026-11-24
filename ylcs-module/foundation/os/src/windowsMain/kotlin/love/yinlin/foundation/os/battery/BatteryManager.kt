package love.yinlin.foundation.os.battery

import love.yinlin.foundation.PlatformContext

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = WindowsBatteryApi.readLevel()
}
