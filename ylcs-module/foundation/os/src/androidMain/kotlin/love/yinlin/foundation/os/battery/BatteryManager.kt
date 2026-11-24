package love.yinlin.foundation.os.battery

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingNull
import love.yinlin.foundation.PlatformContext
import kotlin.time.Duration.Companion.milliseconds
import android.os.BatteryManager as AndroidBatteryManager

actual object BatteryManager {
    actual suspend fun getBatteryInfo(context: PlatformContext): BatteryInfo = Coroutines.io {
        val [intent, power] = catchingNull {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) to context.getSystemService(PowerManager::class.java)
        } ?: (null to null)

        val remaining = if (Build.VERSION.SDK_INT >= 31) power?.batteryDischargePrediction?.toMillis()?.takeIf { it >= 0L }?.milliseconds else null
        val hasBattery = intent?.getBooleanExtra(AndroidBatteryManager.EXTRA_PRESENT, true) ?: false

        BatteryInfo(
            level = when {
                intent == null -> BatteryLevel.Unknown
                !hasBattery -> BatteryLevel.NoBattery
                else -> {
                    val level = intent.getIntExtra(AndroidBatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(AndroidBatteryManager.EXTRA_SCALE, -1)
                    if (scale > 0) BatteryLevel.fromFraction(level.toDouble() / scale) else BatteryLevel.Unknown
                }
            },
            chargingState = if (hasBattery) {
                when (intent.getIntExtra(AndroidBatteryManager.EXTRA_STATUS, -1)) {
                    AndroidBatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargingState.Charging
                    AndroidBatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargingState.Discharging
                    AndroidBatteryManager.BATTERY_STATUS_FULL -> BatteryChargingState.Full
                    AndroidBatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargingState.NotCharging
                    else -> null
                }
            } else null,
            powerSource = when (intent?.getIntExtra(AndroidBatteryManager.EXTRA_PLUGGED, -1)) {
                0 -> if (hasBattery) PowerSource.Battery else null
                AndroidBatteryManager.BATTERY_PLUGGED_AC -> PowerSource.AC
                AndroidBatteryManager.BATTERY_PLUGGED_USB -> PowerSource.USB
                AndroidBatteryManager.BATTERY_PLUGGED_WIRELESS -> PowerSource.Wireless
                AndroidBatteryManager.BATTERY_PLUGGED_DOCK -> PowerSource.Dock
                else -> null
            },
            remainingTime = remaining,
            isPowerSaveMode = power?.isPowerSaveMode,
        )
    }
}
