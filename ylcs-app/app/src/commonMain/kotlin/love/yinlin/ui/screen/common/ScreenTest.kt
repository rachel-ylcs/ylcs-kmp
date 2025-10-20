package love.yinlin.ui.screen.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.AppModel
import love.yinlin.compose.Device
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