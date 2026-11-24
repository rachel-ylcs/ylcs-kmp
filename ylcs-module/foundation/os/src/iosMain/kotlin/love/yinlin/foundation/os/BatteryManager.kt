package love.yinlin.foundation.os

import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext
import platform.UIKit.UIDevice

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.main {
        val device = UIDevice.currentDevice
        val monitoring = device.batteryMonitoringEnabled
        try {
            if (!monitoring) device.batteryMonitoringEnabled = true
            BatteryLevel.fromFraction(device.batteryLevel.toDouble())
        }
        finally {
            if (!monitoring) device.batteryMonitoringEnabled = false
        }
    }
}
