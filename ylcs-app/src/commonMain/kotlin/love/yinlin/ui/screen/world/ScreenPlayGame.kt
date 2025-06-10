package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.data.rachel.game.Game
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenPlayGame(model: AppModel, val args: Args) : SubScreen<ScreenPlayGame.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game)

    override val title: String = args.type.title

    @Composable
    override fun SubContent(device: Device) {

    }
}