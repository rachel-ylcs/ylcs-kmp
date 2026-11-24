package love.yinlin.foundation.os

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import kotlin.math.roundToInt

internal object LinuxBatteryReader {
    private fun read(path: Path, name: String): String? = catchingNull {
        SystemFileSystem.source(Path(path, name)).buffered().use { it.readString().trim() }
    }

    fun readLevel(): BatteryLevel = catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
        val supplies = SystemFileSystem.list(Path("/sys/class/power_supply"))
        var count = 0
        var total = 0
        for (supply in supplies) {
            val type = read(supply, "type") ?: return@catchingDefault BatteryLevel.Unknown
            if (type != "Battery" || read(supply, "scope") == "Device" || read(supply, "present") == "0") continue
            val capacity = read(supply, "capacity")?.toIntOrNull() ?: return@catchingDefault BatteryLevel.Unknown
            if (capacity !in 0 .. 100) return@catchingDefault BatteryLevel.Unknown
            total += capacity
            count++
        }
        // 多块系统电池采用各自电量百分比的算术平均值。
        if (count == 0) BatteryLevel.NoBattery else BatteryLevel.Value((total.toDouble() / count).roundToInt())
    }
}
