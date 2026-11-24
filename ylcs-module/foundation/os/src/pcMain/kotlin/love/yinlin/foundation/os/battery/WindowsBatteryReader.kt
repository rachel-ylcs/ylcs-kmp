package love.yinlin.foundation.os.battery

import kotlin.time.Duration.Companion.seconds

internal object WindowsBatteryReader {
    fun readInfo(ac: Int, flag: Int, percent: Int, saver: Int, seconds: UInt): BatteryInfo {
        val level = BatteryLevel.fromWindows(flag, percent)
        return BatteryInfo(
            level = level,
            chargingState = when {
                flag == 255 || level == BatteryLevel.NoBattery -> null
                (flag and 8) != 0 -> BatteryChargingState.Charging
                ac == 0 -> BatteryChargingState.Discharging
                ac == 1 -> BatteryChargingState.NotCharging
                else -> null
            },
            powerSource = when (ac) {
                0 -> if (level == BatteryLevel.NoBattery) null else PowerSource.Battery
                1 -> PowerSource.External
                else -> null
            },
            remainingTime = seconds.takeIf { it != UInt.MAX_VALUE }?.toLong()?.seconds,
            isPowerSaveMode = when (saver) {
                0 -> false
                1 -> true
                else -> null
            },
        )
    }
}
