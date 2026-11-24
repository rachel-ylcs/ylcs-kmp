package love.yinlin.foundation.os.battery

import love.yinlin.foundation.PlatformContext

expect object BatteryManager {
    suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo
}
