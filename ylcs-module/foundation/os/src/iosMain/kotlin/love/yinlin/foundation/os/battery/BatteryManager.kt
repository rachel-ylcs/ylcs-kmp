package love.yinlin.foundation.os.battery

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContext
import platform.UIKit.UIDevice

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.main {
        catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
            val device = UIDevice.currentDevice
            val monitoring = device.batteryMonitoringEnabled
            cleaning({
                if (!monitoring) device.batteryMonitoringEnabled = true
                BatteryLevel.fromFraction(device.batteryLevel.toDouble())
            }) {
                if (!monitoring) device.batteryMonitoringEnabled = false
            }
        }
    }
}
