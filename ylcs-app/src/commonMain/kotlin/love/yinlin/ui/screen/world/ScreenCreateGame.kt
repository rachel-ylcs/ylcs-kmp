package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.world.game.GameSlider
import love.yinlin.ui.screen.world.game.cast
import love.yinlin.ui.screen.world.game.createGameState

@Stable
class ScreenCreateGame(model: AppModel, val args: Args) : SubScreen<ScreenCreateGame.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game)

    override val title: String = "${args.type.title} - 新游戏"

    private val state = createGameState(args.type, slot)
    private val config = state.config

    private val titleState = TextInputState()
    private var reward by mutableFloatStateOf(0f)
    private var num by mutableFloatStateOf(1f)
    private var cost by mutableFloatStateOf(0f)
    private val maxCost by derivedStateOf { reward.cast(config.minReward, config.maxReward) / config.maxCostRatio }

    private val canSubmit by derivedStateOf { titleState.ok && state.canSubmit }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Check,
            enabled = canSubmit
        ) {
            val profile = app.config.userProfile
            if (profile != null) {
                val reward = reward.cast(config.minReward, config.maxReward)
                val actionCoin = (reward * GameConfig.rewardCostRatio).toInt()
                if (profile.coin >= actionCoin) {
                    val result = ClientAPI.request(
                        route = API.User.Game.CreateGame,
                        data = API.User.Game.CreateGame.Request(
                            token = app.config.userToken,
                            title = titleState.text,
                            type = args.type,
                            reward = reward,
                            num = num.cast(config.minRank, config.maxRank),
                            cost = cost.cast(0, maxCost),
                            info = state.submitInfo,
                            question = state.submitQuestion,
                            answer = state.submitAnswer,
                        )
                    )
                    when (result) {
                        is Data.Success -> {
                            worldPart.slot.tip.success(result.message)
                            app.config.userProfile = profile.copy(coin = profile.coin - actionCoin)
                            pop()
                        }
                        is Data.Error -> slot.tip.error(result.message)
                    }
                }
                else slot.tip.warning("银币不足够支持${(GameConfig.rewardCostRatio * 100).toInt()}%=${actionCoin}的奖励")
            }
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        Column(
            modifier = Modifier.padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(ThemeValue.Padding.EqualValue)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace),
        ) {
            TextInput(
                state = titleState,
                hint = "标题",
                maxLines = 3,
                minLines = 1,
                maxLength = 128,
                clearButton = false,
                modifier = Modifier.fillMaxWidth()
            )
            GameSlider(
                title = "奖励银币\n(+20%)",
                progress = reward,
                minValue = config.minReward,
                maxValue = config.maxReward,
                onProgressChange = { reward = it },
                modifier = Modifier.fillMaxWidth()
            )
            GameSlider(
                title = "限定名额",
                progress = num,
                minValue = config.minRank,
                maxValue = config.maxRank,
                onProgressChange = { num = it },
                modifier = Modifier.fillMaxWidth()
            )
            GameSlider(
                title = "入场银币",
                progress = cost,
                minValue = 0,
                maxValue = maxCost,
                onProgressChange = { cost = it },
                modifier = Modifier.fillMaxWidth()
            )
            with(state) { Content() }
        }
    }

    @Composable
    override fun Floating() {
        state.Floating()
    }
}