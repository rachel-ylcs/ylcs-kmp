package love.yinlin.foundation.os.battery

import kotlin.test.assertTrue

internal fun assertBatteryLevelResult(level: BatteryLevel, allowUnsupported: Boolean = false) {
    println("BatteryManager returned: $level")
    assertTrue(
        when (level) {
            is BatteryLevel.Value -> level.percent in 0 .. 100
            BatteryLevel.NoBattery, BatteryLevel.Unknown -> true
            BatteryLevel.Unsupported -> allowUnsupported
        },
        "Unexpected battery level: $level",
    )
}
