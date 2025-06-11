package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenGameHistory(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "我的游戏"

    @Composable
    override fun SubContent(device: Device) {

    }
}