package love.yinlin.ui.screen.world

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.platform.app
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenPlayGame(model: AppModel, val args: Args) : SubScreen<ScreenPlayGame.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game, val gid: Int)

    override val title: String = args.type.title

    private fun preflight() {
        launch {
            val result = ClientAPI.request(
                route = API.User.Game.PreflightGame,
                data = API.User.Game.PreflightGame.Request(
                    token = app.config.userToken,
                    gid = args.gid
                )
            )
            when (result) {
                is Data.Success -> {
                    val preflightResult = result.data
                }
                is Data.Error -> slot.tip.error(result.message)
            }
        }
    }

    override suspend fun initialize() {

    }

    @Composable
    override fun SubContent(device: Device) {

    }
}