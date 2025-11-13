package love.yinlin.screen.world

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import love.yinlin.Local
import love.yinlin.api.API2
import love.yinlin.api.ClientAPI2
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.LoadingSecondaryButton
import love.yinlin.compose.ui.layout.StatusBox
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Space
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.world.game.GameItem
import love.yinlin.screen.world.game.playGameState

@Stable
class ScreenPlayGame(manager: ScreenManager) : Screen(manager) {
    @Stable
    private enum class Status {
        Preparing, Playing, Settling
    }

    private val game: GamePublicDetailsWithName? = manager.get<ScreenMain>().get<SubScreenWorld>().currentGame

    private var status by mutableStateOf(Status.Preparing)

    private val state = playGameState(game?.type ?: Game.AnswerQuestion, slot)

    private var preflightResult: PreflightResult? by mutableRefStateOf(null)
    private var gameResult: GameResult? by mutableRefStateOf(null)

    private val canSubmit by derivedStateOf { status == Status.Playing && preflightResult != null && state.canSubmit }

    private suspend fun preflight() {
        val result = ClientAPI2.request(
            route = API2.User.Game.PreflightGame,
            data = API2.User.Game.PreflightGame.Request(
                token = app.config.userToken,
                gid = game?.gid ?: 0
            )
        )
        when (result) {
            is Data.Success -> {
                preflightResult = result.data
                state.init(viewModelScope, result.data)
                status = Status.Playing
            }
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    @Composable
    private fun GameLayout(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.TopCenter
        ) {
            when (status) {
                Status.Preparing -> {
                    LoadingSecondaryButton(
                        text = "开始",
                        modifier = Modifier.padding(CustomTheme.padding.verticalSpace),
                        onClick = { preflight() }
                    )
                }
                Status.Playing -> {
                    preflightResult?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            shadowElevation = CustomTheme.shadow.surface
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(CustomTheme.padding.equalExtraValue)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
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
                            shape = MaterialTheme.shapes.extraLarge,
                            shadowElevation = CustomTheme.shadow.surface
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(CustomTheme.padding.equalExtraValue)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBox(ok = result.isCompleted, size = CustomTheme.size.largeImage)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace * 1.5f)
                                    ) {
                                        Text(
                                            text = if (result.isCompleted) "成功" else "失败",
                                            style = MaterialTheme.typography.displayMedium,
                                            color = if (result.isCompleted) Colors.Green4 else Colors.Red4,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        SecondaryButton(
                                            text = "返回",
                                            onClick = {
                                                preflightResult = null
                                                gameResult = null
                                                status = Status.Preparing
                                            }
                                        )
                                    }
                                }
                                Space()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    NormalText(text = result.reward.toString(), icon = Icons.Outlined.Diamond)
                                    NormalText(text = result.rank.toString(), icon = Icons.Outlined.FormatListNumbered)
                                }
                                Space()
                                with(state) { Settlement() }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait(details: GamePublicDetailsWithName) {
        Column(
            modifier = Modifier.fillMaxSize().padding(CustomTheme.padding.equalExtraValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            GameItem(
                game = details,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
            GameLayout(modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun Landscape(details: GamePublicDetailsWithName) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
        ) {
            Column(
                modifier = Modifier.width(CustomTheme.size.panelWidth).fillMaxHeight()
                    .padding(CustomTheme.padding.equalExtraValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
            ) {
                GameItem(
                    game = details,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                )
            }
            Column(
                modifier = Modifier.width(CustomTheme.size.panelWidth).fillMaxHeight()
                    .padding(CustomTheme.padding.equalExtraValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
            ) {
                GameLayout(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    override val title: String = game?.type?.title ?: "未知游戏"

    override fun onBack() {
        if (status == Status.Playing) {
            launch {
                if (slot.confirm.openSuspend(content = "中途退出将以失败结算")) pop()
            }
        }
        else pop()
    }

    @Composable
    override fun ActionScope.RightActions() {
        if (game != null && status == Status.Playing) {
            ActionSuspend(
                icon = Icons.Outlined.Check,
                tip = "提交",
                enabled = canSubmit
            ) {
                preflightResult?.let { preflight ->
                    val result = ClientAPI2.request(
                        route = API2.User.Game.VerifyGame,
                        data = API2.User.Game.VerifyGame.Request(
                            token = app.config.userToken,
                            gid = game.gid,
                            rid = preflight.rid,
                            answer = state.submitAnswer
                        )
                    )
                    when (result) {
                        is Data.Success -> {
                            gameResult = result.data
                            state.settle(result.data)
                            preflightResult = null
                            status = Status.Settling
                        }
                        is Data.Failure -> slot.tip.error(result.message)
                    }
                }
            }
        }
    }

    @Composable
    override fun Content(device: Device) {
        game?.let { details ->
            Box(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
                val deviceType = LocalDevice.current.type

                WebImage(
                    uri = remember(deviceType) { game.type.xyPath(deviceType != Device.Type.PORTRAIT) },
                    key = Local.info.version,
                    contentScale = ContentScale.Crop,
                    alpha = 0.75f,
                    modifier = Modifier.fillMaxSize().zIndex(1f)
                )

                Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                    when (deviceType) {
                        Device.Type.PORTRAIT -> Portrait(details)
                        Device.Type.SQUARE, Device.Type.LANDSCAPE -> Landscape(details)
                    }
                }
            }
        }
    }

    @Composable
    override fun Floating() {
        state.Floating()
    }
}