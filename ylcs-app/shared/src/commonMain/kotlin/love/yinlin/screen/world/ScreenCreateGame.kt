package love.yinlin.screen.world

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.cs.*
import love.yinlin.data.rachel.game.CreateGameArgs
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.world.game.GameSlider
import love.yinlin.screen.world.game.cast
import love.yinlin.screen.world.game.createGameState

@Stable
class ScreenCreateGame(manager: ScreenManager, private val type: Game) : Screen(manager) {
    private val subScreenWorld = manager.get<ScreenMain>().get<SubScreenWorld>()

    private val state = createGameState(type, slot)
    private val config = state.config

    private val titleState = TextInputState()
    private var reward by mutableFloatStateOf(0f)
    private var num by mutableFloatStateOf(1f)
    private var cost by mutableFloatStateOf(0f)
    private val maxCost by derivedStateOf { reward.cast(config.minReward, config.maxReward) / config.maxCostRatio }
    private val maxNum by derivedStateOf {
        val actualCost = cost.cast(0, maxCost)
        if (actualCost == 0) config.maxRank else (reward.cast(config.minReward, config.maxReward) / actualCost / 3).coerceAtMost(config.maxRank)
    }

    private val canSubmit by derivedStateOf { titleState.ok && state.canSubmit }

    @Composable
    private fun ColumnScope.ArgsLayout() {
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
            title = "奖励银币(手续+20%)",
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
            maxValue = maxNum,
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
    }

    @Composable
    private fun Portrait() {
        Column(
            modifier = Modifier.padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(CustomTheme.padding.equalValue)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            ArgsLayout()
            with(state) { this@Column.Content() }
        }
    }

    @Composable
    private fun Landscape() {
        Row(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
        ) {
            Column(
                modifier = Modifier.width(CustomTheme.size.panelWidth).fillMaxHeight()
                    .padding(CustomTheme.padding.equalValue)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                ArgsLayout()
            }
            Column(
                modifier = Modifier.width(CustomTheme.size.panelWidth).fillMaxHeight()
                    .padding(CustomTheme.padding.equalValue)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                with(state) { this@Column.Content() }
            }
        }
    }

    override val title: String = "${type.title} - 新游戏"

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Check,
            tip = "提交",
            enabled = canSubmit
        ) {
            val profile = app.config.userProfile
            if (profile != null) {
                val reward = reward.cast(config.minReward, config.maxReward)
                val actionCoin = (reward * GameConfig.rewardCostRatio).toInt()
                if (profile.coin >= actionCoin) {
                    ApiGameCreateGame.request(app.config.userToken, CreateGameArgs(
                        title = titleState.text,
                        type = type,
                        reward = reward,
                        num = num.cast(config.minRank, maxNum),
                        cost = cost.cast(0, maxCost),
                        info = state.submitInfo,
                        question = state.submitQuestion,
                        answer = state.submitAnswer,
                    )) {
                        subScreenWorld.slot.tip.success("创建成功")
                        app.config.userProfile = profile.copy(coin = profile.coin - actionCoin)
                        pop()
                    }.errorTip
                }
                else slot.tip.warning("银币不足够支持${(GameConfig.rewardCostRatio * 100).toInt()}%=${actionCoin}的奖励")
            }
        }
    }

    @Composable
    override fun Content(device: Device) {
        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT -> Portrait()
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
        }
    }

    @Composable
    override fun Floating() {
        state.Floating()
    }
}