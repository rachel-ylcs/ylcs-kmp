package love.yinlin.foundation.os.battery

import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext

actual object BatteryManager {
    actual suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo = Coroutines.io {
        LinuxBatteryReader.readInfo()
    }
}
