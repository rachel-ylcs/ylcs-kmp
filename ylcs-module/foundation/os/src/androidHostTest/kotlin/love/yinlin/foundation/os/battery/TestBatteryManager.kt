package love.yinlin.foundation.os.battery

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import love.yinlin.foundation.PlatformContext
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class TestBatteryManager {
    @Test
    fun readsActualBatteryLevel() = runTest {
        val context = ApplicationProvider.getApplicationContext<PlatformContext>()
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
