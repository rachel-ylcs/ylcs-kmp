package love.yinlin.foundation.os.battery

import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

abstract class MacBatteryReader<T : Any> {
    protected abstract fun copyInfo(): T?
    protected abstract fun copySources(info: T): T?
    protected abstract fun count(sources: T): Long
    protected abstract fun description(info: T, sources: T, index: Long): T?
    protected abstract fun matches(source: T, key: String, expected: String): Boolean?
    protected abstract fun boolean(source: T, key: String): Boolean?
    protected abstract fun number(source: T, key: String): Int?
    protected abstract fun powerSaveMode(): Boolean?
    protected abstract fun release(value: T)

    private fun readPowerSource(battery: T): PowerSource? = when {
        matches(battery, "Power Source State", "AC Power") == true -> PowerSource.External
        matches(battery, "Power Source State", "Battery Power") == true -> PowerSource.Battery
        else -> null
    }

    private fun readBatteries(fallback: BatteryInfo, read: (List<T>) -> BatteryInfo): BatteryInfo = catchingDefault(fallback) {
        val info = copyInfo() ?: return@catchingDefault fallback
        cleaning({
            val sources = copySources(info) ?: return@cleaning fallback
            cleaning({
                val batteries = ArrayList<T>()
                for (index in 0L ..< count(sources)) {
                    val battery = description(info, sources, index) ?: return@cleaning fallback
                    val internal = matches(battery, "Transport Type", "Internal") ?: return@cleaning fallback
                    if (internal && boolean(battery, "Is Present") != false) batteries.add(battery)
                }
                read(batteries)
            }) {
                release(sources)
            }
        }) {
            release(info)
        }
    }

    fun readInfo(): BatteryInfo {
        val powerSave = catchingNull { powerSaveMode() }
        val fallback = BatteryInfo(isPowerSaveMode = powerSave)

        return readBatteries(fallback) { batteries ->
            val levels = batteries.map { battery ->
                val current = number(battery, "Current Capacity")
                val maximum = number(battery, "Max Capacity")
                if (current != null && maximum != null && maximum > 0 && current in 0 .. maximum) 100.0 * current / maximum else null
            }

            val level = when {
                levels.isEmpty() -> BatteryLevel.NoBattery
                levels.any { it == null } -> BatteryLevel.Unknown
                else -> BatteryLevel.Value(levels.filterNotNull().average().roundToInt())
            }

            val source = batteries.map { battery -> readPowerSource(battery) }.distinct().singleOrNull()

            val charging = batteries.map { battery ->
                when {
                    boolean(battery, "Is Charging") == true -> BatteryChargingState.Charging
                    readPowerSource(battery) == PowerSource.Battery -> BatteryChargingState.Discharging
                    boolean(battery, "Is Charged") == true -> BatteryChargingState.Full
                    boolean(battery, "Is Charging") == false -> BatteryChargingState.NotCharging
                    else -> null
                }
            }.distinct().singleOrNull()

            val remaining = batteries.singleOrNull()?.takeIf { source == PowerSource.Battery && charging != BatteryChargingState.Charging }?.let { number(it, "Time to Empty") }?.takeIf { it >= 0 }?.minutes

            BatteryInfo(level, charging, source, remaining, powerSave)
        }
    }
}
