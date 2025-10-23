package love.yinlin.screen.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.compose.Device
import love.yinlin.compose.screen.CommonScreen
import love.yinlin.compose.screen.ScreenManager

@Stable
class ScreenTest(manager: ScreenManager) : CommonScreen(manager) {
    override val title: String = "测试页"

    override suspend fun initialize() {

    }

    @Composable
    override fun Content(device: Device) {

    }
}