package love.yinlin.foundation.os.battery

import love.yinlin.extension.catchingDefault
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt

internal abstract class MacBatteryReader<T : Any> {
    protected abstract fun copyInfo(): T?
    protected abstract fun copySources(info: T): T?
    protected abstract fun count(sources: T): Long
    protected abstract fun description(info: T, sources: T, index: Long): T?
    protected abstract fun isInternal(source: T): Boolean?
    protected abstract fun isPresent(source: T): Boolean
    protected abstract fun number(source: T, key: String): Int?
    protected abstract fun release(value: T)

    fun readLevel(): BatteryLevel = catchingDefault<BatteryLevel>(BatteryLevel.Unknown) {
        val info = copyInfo() ?: return@catchingDefault BatteryLevel.Unknown
        cleaning({
            val sources = copySources(info) ?: return@cleaning BatteryLevel.Unknown
            cleaning({
                readSources(info, sources)
            }) {
                release(sources)
            }
        }) {
            release(info)
        }
    }

    private fun readSources(info: T, sources: T): BatteryLevel {
        var total = 0.0
        var batteries = 0
        for (index in 0L ..< count(sources)) {
            val source = description(info, sources, index) ?: return BatteryLevel.Unknown
            val internal = isInternal(source) ?: return BatteryLevel.Unknown
            if (!internal || !isPresent(source)) continue
            val current = number(source, "Current Capacity") ?: return BatteryLevel.Unknown
            val maximum = number(source, "Max Capacity") ?: return BatteryLevel.Unknown
            if (maximum <= 0 || current !in 0 .. maximum) return BatteryLevel.Unknown
            total += 100.0 * current / maximum
            batteries++
        }
        return if (batteries == 0) BatteryLevel.NoBattery else BatteryLevel.Value((total / batteries).roundToInt())
    }
}
