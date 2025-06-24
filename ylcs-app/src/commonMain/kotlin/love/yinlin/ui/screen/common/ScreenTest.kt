package love.yinlin.ui.screen.common

import androidx.compose.runtime.*
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    override suspend fun initialize() {

    }

    @Composable
    override fun SubContent(device: Device) {

    }
}