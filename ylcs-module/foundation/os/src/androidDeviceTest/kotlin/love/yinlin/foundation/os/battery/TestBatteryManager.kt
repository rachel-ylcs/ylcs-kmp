package love.yinlin.foundation.os.battery

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import love.yinlin.foundation.PlatformContext
import kotlin.test.Test

class TestBatteryManager {
    @Test
    fun readsActualBatteryLevel() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PlatformContext
        val level = BatteryManager.getBatteryLevel(context)
        assertBatteryLevelResult(level)
    }
}
