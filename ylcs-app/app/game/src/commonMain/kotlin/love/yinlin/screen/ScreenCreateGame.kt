package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.common.GameMapper
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.ArgsSlider
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.cs.ApiGameCreateGame
import love.yinlin.cs.request
import love.yinlin.data.rachel.game.CreateGameArgs
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameConfig

@Stable
class ScreenCreateGame(private val game: Game) : Screen() {
    @Stable
    data class GameConfigArgs(
        private val config: GameConfig,
        private val tmpReward: Int = config.minReward,
        private val tmpCost: Int = 0,
        private val tmpNum: Int = config.minRank
    ) {
        private val minReward: Int = config.minReward
        private val maxReward: Int = config.maxReward
        val reward: Int = tmpReward.coerceIn(minReward, maxReward)
        val rewardArgs = SliderArgs(reward, minReward, maxReward)
        private val minCost: Int = 0
        private val maxCost: Int = (reward / config.maxCostRatio).coerceAtLeast(minCost)
        val cost: Int = tmpCost.coerceIn(minCost, maxCost)
        val costArgs = SliderArgs(cost, minCost, maxCost)
        private val minNum: Int = config.minRank
        private val maxNum: Int = if (cost == 0) config.maxRank else (reward / cost / 3).coerceIn(minNum, config.maxRank)
        val num: Int = tmpNum.coerceIn(minNum, maxNum)
        val numArgs = SliderArgs(num, minNum, maxNum)
    }

    private val state = GameMapper.cast<GameMapper>(game)!!.gameCreator!!.invoke(this)
    private val config = state.config

    private val gameTitle = InputState(maxLength = 128)
    private var args by mutableRefStateOf(GameConfigArgs(config))

    private val canSubmit by derivedStateOf { gameTitle.isSafe && state.canSubmit }

    override val title: String = "${game.title} - 创建"

    private val baseInfoLayout = movableComposable { scope: ColumnScope ->
        // TitleLayout
        Input(
            state = gameTitle,
            hint = "标题与介绍",
            maxLines = 3,
            minLines = 1,
            modifier = Modifier.fillMaxWidth()
        )

        // BasicConfigLayout
        ArgsSlider(
            title = "奖励银币(手续+20%)",
            args = args.rewardArgs,
            onValueChange = { args = args.copy(tmpReward = it) },
            modifier = Modifier.fillMaxWidth()
        )
        ArgsSlider(
            title = "限定名额",
            args = args.numArgs,
            onValueChange = { args = args.copy(tmpNum = it) },
            modifier = Modifier.fillMaxWidth()
        )
        ArgsSlider(
            title = "入场银币",
            args = args.costArgs,
            onValueChange = { args = args.copy(tmpCost = it) },
            modifier = Modifier.fillMaxWidth()
        )

        // ConfigContent
        with(state) { scope.ConfigContent() }
    }

    private val extraInfoLayout = movableComposable { scope: ColumnScope ->
        with(state) { scope.Content() }
    }

    @Composable
    private fun Portrait() {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(Theme.padding.eValue)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            baseInfoLayout(this)
            extraInfoLayout(this)
        }
    }

    @Composable
    private fun Landscape() {
        Row(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
        ) {
            Column(
                modifier = Modifier.width(Theme.size.cell1).fillMaxHeight()
                    .padding(Theme.padding.eValue)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                baseInfoLayout(this)
            }
            Column(
                modifier = Modifier.width(Theme.size.cell1).fillMaxHeight()
                    .padding(Theme.padding.eValue)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                extraInfoLayout(this)
            }
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Check, tip = "提交", enabled = canSubmit, onClick = {
            val profile = app.config.userProfile
            if (profile != null) {
                val actionCoin = (args.reward * GameConfig.rewardCostRatio).toInt()
                if (profile.coin >= actionCoin) {
                    ApiGameCreateGame.request(app.config.userToken, CreateGameArgs(
                        title = gameTitle.text,
                        type = game,
                        reward = args.reward,
                        num = args.num,
                        cost = args.cost,
                        info = state.submitInfo,
                        question = state.submitQuestion,
                        answer = state.submitAnswer,
                    )) {
                        app.config.userProfile = profile.copy(coin = profile.coin - actionCoin)
                        pop()
                    }.errorTip
                }
                else slot.tip.warning("银币不足够支持${(GameConfig.rewardCostRatio * 100).toInt()}%=${actionCoin}的奖励")
            }
        })
    }

    @Composable
    override fun Content() {
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