package love.yinlin.foundation.os.battery

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContext
import platform.Foundation.NSProcessInfo
import platform.Foundation.lowPowerModeEnabled
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState

actual object BatteryManager {
    actual suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo = Coroutines.main {
        val powerSave = catchingNull { NSProcessInfo.processInfo.lowPowerModeEnabled }

        catchingDefault(BatteryInfo(isPowerSaveMode = powerSave)) {
            val device = UIDevice.currentDevice
            val monitoring = device.batteryMonitoringEnabled

            cleaning({
                if (!monitoring) device.batteryMonitoringEnabled = true
                val state = device.batteryState

                BatteryInfo(
                    level = BatteryLevel.fromFraction(device.batteryLevel.toDouble()),
                    chargingState = when (state) {
                        UIDeviceBatteryState.UIDeviceBatteryStateCharging -> BatteryChargingState.Charging
                        UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> BatteryChargingState.Discharging
                        UIDeviceBatteryState.UIDeviceBatteryStateFull -> BatteryChargingState.Full
                        else -> null
                    },
                    powerSource = when (state) {
                        UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> PowerSource.Battery
                        UIDeviceBatteryState.UIDeviceBatteryStateCharging, UIDeviceBatteryState.UIDeviceBatteryStateFull -> PowerSource.External
                        else -> null
                    },
                    isPowerSaveMode = powerSave
                )
            }) {
                if (!monitoring) device.batteryMonitoringEnabled = false
            }
        }
    }
}
