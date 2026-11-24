package love.yinlin.foundation.os.battery

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import love.yinlin.foundation.PlatformContext
import kotlin.test.Test
import kotlin.test.assertTrue


class TestBatteryManager {
    @Test
    fun readsActualBatteryLevel() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PlatformContext
        val level = BatteryManager.getBatteryLevel(context)
        assertTrue(
            when (level) {
                is BatteryLevel.Value -> level.percent in 0 .. 100
                BatteryLevel.NoBattery, BatteryLevel.Unknown -> true
                BatteryLevel.Unsupported -> false
            },
            "Unexpected battery level: $level",
        )
    }
}
