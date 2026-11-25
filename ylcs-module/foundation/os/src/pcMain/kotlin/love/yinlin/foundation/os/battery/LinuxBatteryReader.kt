package love.yinlin.foundation.os.battery

import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.fs.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

internal object LinuxBatteryReader {
    private val suppliesPath = File("/sys/class/power_supply")

    private suspend fun read(path: File, name: String): String? = catchingNull {
        File(path, name).readText()?.trim()?.takeIf { it.isNotEmpty() }
    }

    suspend fun readInfo(): BatteryInfo = catchingDefault(BatteryInfo()) {
        val levels: MutableList<Int?> = []
        val states: MutableList<BatteryChargingState?> = []
        val times: MutableList<Long?> = []
        val externalSources: MutableSet<PowerSource> = []
        var unknownSupply = false

        for (supply in suppliesPath.list()) {
            val properties = mutableMapOf<String, String>()

            read(supply, "uevent")?.lineSequence()?.forEach { line ->
                val split = line.indexOf('=')
                if (split > 0) properties[line.substring(0, split).removePrefix("POWER_SUPPLY_")] = line.substring(split + 1)
            }

            when (val type = properties["TYPE"] ?: read(supply, "type")) {
                null -> unknownSupply = true
                "Battery" -> {
                    val isSystemBattery = (properties["SCOPE"] ?: read(supply, "scope")) != "Device" && (properties["PRESENT"] ?: read(supply, "present")) != "0"
                    if (isSystemBattery) {
                        levels.add((properties["CAPACITY"] ?: read(supply, "capacity"))?.toIntOrNull()?.takeIf { it in 0 .. 100 })
                        states.add(when (properties["STATUS"] ?: read(supply, "status")) {
                            "Charging" -> BatteryChargingState.Charging
                            "Discharging" -> BatteryChargingState.Discharging
                            "Full" -> BatteryChargingState.Full
                            "Not charging" -> BatteryChargingState.NotCharging
                            else -> null
                        })

                        val emptyNow = (properties["TIME_TO_EMPTY_NOW"] ?: read(supply, "time_to_empty_now"))?.toLongOrNull()?.takeIf { it > 0 }
                        val emptyAvg = (properties["TIME_TO_EMPTY_AVG"] ?: read(supply, "time_to_empty_avg"))?.toLongOrNull()?.takeIf { it > 0 }
                        times.add(emptyNow ?: emptyAvg)
                    }
                }
                else -> {
                    val online = (properties["ONLINE"] ?: read(supply, "online"))?.toIntOrNull()
                    if (online != null && online > 0) {
                        externalSources.add(when {
                            type == "Mains" -> PowerSource.AC
                            type == "Wireless" -> PowerSource.Wireless
                            type == "UPS" -> PowerSource.UPS
                            type.startsWith("USB") -> PowerSource.USB
                            else -> PowerSource.External
                        })
                    }
                }
            }
        }

        val level = when {
            unknownSupply || levels.any { it == null } -> BatteryLevel.Unknown
            levels.isEmpty() -> BatteryLevel.NoBattery
            else -> BatteryLevel.Value(levels.filterNotNull().average().roundToInt())
        }

        val charging = states.distinct().singleOrNull()

        val source = when {
            externalSources.size == 1 -> externalSources.single()
            externalSources.isNotEmpty() -> PowerSource.External
            charging == BatteryChargingState.Discharging -> PowerSource.Battery
            charging == BatteryChargingState.Charging -> PowerSource.External
            else -> null
        }

        val remaining = times.singleOrNull()?.takeIf { charging == BatteryChargingState.Discharging && source == PowerSource.Battery }?.seconds

        BatteryInfo(level, charging, source, remaining)
    }
}
