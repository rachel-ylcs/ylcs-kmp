package love.yinlin.foundation.os.battery

import kotlinx.coroutines.test.runTest
import love.yinlin.foundation.PlatformContext
import kotlin.test.Test

class TestBatteryManager {
    @Test
    fun readsActualBatteryLevel() = runTest {
        val level = BatteryManager.getBatteryLevel(PlatformContext.Instance)
        assertBatteryLevelResult(level, allowUnsupported = true)
    }
}
