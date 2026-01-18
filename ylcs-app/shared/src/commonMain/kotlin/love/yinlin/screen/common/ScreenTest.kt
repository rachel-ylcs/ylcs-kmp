package love.yinlin.screen.common

import androidx.compose.runtime.*
import love.yinlin.compose.Device
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.PAG

@Stable
class ScreenTest(manager: ScreenManager) : Screen(manager) {
    override val title: String = "测试页"

    override suspend fun initialize() {
        PAG.init()
        println(PAG.sdkVersion)
    }

    @Composable
    override fun Content(device: Device) {
        
    }
}