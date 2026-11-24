package love.yinlin.foundation.os.battery

import android.content.Intent
import android.content.IntentFilter
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import android.os.BatteryManager as AndroidBatteryManager

actual object BatteryManager {
    actual suspend fun getBatteryLevel(context: PlatformContext): BatteryLevel = Coroutines.io {
        catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return@catchingDefault BatteryLevel.Unknown
            if (intent.hasExtra(AndroidBatteryManager.EXTRA_PRESENT) && !intent.getBooleanExtra(AndroidBatteryManager.EXTRA_PRESENT, true)) return@catchingDefault BatteryLevel.NoBattery

            val level = intent.getIntExtra(AndroidBatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(AndroidBatteryManager.EXTRA_SCALE, -1)
            if (scale > 0) BatteryLevel.fromFraction(level.toDouble() / scale) else BatteryLevel.Unknown
        }
    }
}
