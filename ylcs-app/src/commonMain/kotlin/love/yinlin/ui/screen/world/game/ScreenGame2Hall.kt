package love.yinlin.ui.screen.world.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.data.rachel.game.Game
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenGame2Hall(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "${Game.BlockText.title} - 大厅"

    @Composable
    override fun SubContent(device: Device) {

    }
}