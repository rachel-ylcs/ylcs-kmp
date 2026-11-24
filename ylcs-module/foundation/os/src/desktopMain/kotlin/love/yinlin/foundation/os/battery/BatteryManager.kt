package love.yinlin.foundation.os.battery

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.io {
        catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
            when (platform) {
                Platform.Windows -> WindowsBatteryApi.readLevel()
                Platform.Linux -> LinuxBatteryApi.readLevel()
                Platform.MacOS -> MacBatteryApi.readLevel()
                else -> BatteryLevel.Unsupported
            }
        }
    }
}
