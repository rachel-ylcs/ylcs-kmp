package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.data.rachel.game.Game
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenGameRanking(model: AppModel, val args: Args) : SubScreen<ScreenGameRanking.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game)

    override val title: String = "${args.type.title} - 排行榜"

    @Composable
    override fun SubContent(device: Device) {

    }
}