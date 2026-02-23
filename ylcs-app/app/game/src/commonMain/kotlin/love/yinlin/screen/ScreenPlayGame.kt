package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.common.GameMapper
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.GameItem
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.cs.ApiGamePreflightGame
import love.yinlin.cs.ApiGameVerifyGame
import love.yinlin.cs.request
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult

@Stable
class ScreenPlayGame(private val gameDetails: GamePublicDetailsWithName) : Screen() {
    @Stable
    private enum class Status {
        Preparing, Playing, Settling
    }

    private val state = GameMapper.cast<GameMapper>(gameDetails.type)!!.gamePlayer!!.invoke(this)

    private var status by mutableStateOf(Status.Preparing)

    private var preflightResult: PreflightResult? by mutableRefStateOf(null)
    private var gameResult: GameResult? by mutableRefStateOf(null)

    private val canSubmit by derivedStateOf { status == Status.Playing && preflightResult != null && state.canSubmit }

    private suspend fun preflight() {
        ApiGamePreflightGame.request(app.config.userToken, gameDetails.gid) {
            preflightResult = it
            state.init(it)
            status = Status.Playing
        }.errorTip
    }

    override val title: String = gameDetails.type.title

    override fun onBack() {
        if (status == Status.Playing) {
            launch {
                if (slot.confirm.open(content = "中途退出将以失败结算")) pop()
            }
        }
        else pop()
    }

    @Composable
    private fun GameLayout(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.TopCenter,
        ) {
            when (status) {
                Status.Preparing -> {
                    PrimaryLoadingButton(text = "开始", icon = Icons.Check, onClick = ::preflight)
                }
                Status.Playing -> {
                    if (preflightResult != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Theme.shape.v3,
                            contentPadding = Theme.padding.eValue9,
                            shadowElevation = Theme.shadow.v3,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                            ) {
                                with(state) { Content() }
                            }
                        }
                    }
                }
                Status.Settling -> {
                    gameResult?.let { result ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Theme.shape.v3,
                            contentPadding = Theme.padding.eValue9,
                            shadowElevation = Theme.shadow.v3,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ThemeContainer(if (result.isCompleted) Colors.Green5 else Colors.Red5) {
                                        TextIconAdapter { idIcon, idText ->
                                            Icon(icon = if (result.isCompleted) Icons.Check else Icons.Error, modifier = Modifier.idIcon())
                                            SimpleEllipsisText(text = if (result.isCompleted) "成功" else "失败", style = Theme.typography.v7.bold, modifier = Modifier.idText())
                                        }
                                    }
                                    TextIconAdapter { idIcon, idText ->
                                        Icon(icon = Icons.Diamond, modifier = Modifier.idIcon())
                                        SimpleEllipsisText(text = "奖励 ${result.reward}", modifier = Modifier.idText())
                                    }
                                    TextIconAdapter { idIcon, idText ->
                                        Icon(icon = Icons.FormatListNumbered, modifier = Modifier.idIcon())
                                        SimpleEllipsisText(text = "名次 ${result.rank}", modifier = Modifier.idText())
                                    }
                                }

                                with(state) { Settlement() }

                                SecondaryButton(text = "返回", icon = Icons.ArrowBack, onClick = {
                                    preflightResult = null
                                    gameResult = null
                                    status = Status.Preparing
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait() {
        Column(
            modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue9).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.e7)
        ) {
            GameItem(
                gameDetails = gameDetails,
                modifier = Modifier.fillMaxWidth(),
                onClick = { }
            )
            GameLayout(modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun Landscape() {
        Row(
            modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue9),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e7)
        ) {
            GameItem(
                gameDetails = gameDetails,
                modifier = Modifier.width(Theme.size.cell1),
                onClick = { }
            )
            Column(
                modifier = Modifier.width(Theme.size.cell1).fillMaxHeight().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                GameLayout(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        if (status == Status.Playing) {
            LoadingIcon(icon = Icons.Check, tip = "提交", enabled = canSubmit, onClick = {
                preflightResult?.let { preflight ->
                    ApiGameVerifyGame.request(app.config.userToken, gameDetails.gid, preflight.rid, state.submitAnswer) {
                        gameResult = it
                        state.settle(it)
                        preflightResult = null
                        status = Status.Settling
                    }.errorTip
                }
            })
        }
    }

    @Composable
    override fun Content() {
        Box(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> Portrait()
                Device.Type.SQUARE, Device.Type.LANDSCAPE -> Landscape()
            }
        }
    }

    @Composable
    override fun Floating() {
        state.Floating()
    }
}