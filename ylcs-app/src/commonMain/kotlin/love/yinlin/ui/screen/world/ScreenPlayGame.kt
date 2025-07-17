package love.yinlin.ui.screen.world

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
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.*
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.extension.mutableRefStateOf
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.img_state_loading
import love.yinlin.resources.img_state_network_error
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.input.SecondaryButton
import love.yinlin.ui.component.input.SecondaryLoadingButton
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.screen.world.game.GameItem
import love.yinlin.ui.screen.world.game.playGameState

@Stable
class ScreenPlayGame(model: AppModel) : CommonSubScreen(model) {
    @Stable
    private enum class Status {
        Preparing, Playing, Settling
    }

    private val game: GamePublicDetailsWithName? = worldPart.currentGame

    private var status by mutableStateOf(Status.Preparing)

    private val state = playGameState(game?.type ?: Game.AnswerQuestion, slot)

    private var preflightResult: PreflightResult? by mutableRefStateOf(null)
    private var gameResult: GameResult? by mutableRefStateOf(null)

    private val canSubmit by derivedStateOf { status == Status.Playing && preflightResult != null && state.canSubmit }

    private suspend fun preflight() {
        val result = ClientAPI.request(
            route = API.User.Game.PreflightGame,
            data = API.User.Game.PreflightGame.Request(
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
                    SecondaryLoadingButton(
                        text = "开始",
                        modifier = Modifier.padding(ThemeValue.Padding.VerticalSpace),
                        onClick = { preflight() }
                    )
                }
                Status.Playing -> {
                    preflightResult?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            shadowElevation = ThemeValue.Shadow.Surface
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(ThemeValue.Padding.EqualExtraValue)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
                            shadowElevation = ThemeValue.Shadow.Surface
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(ThemeValue.Padding.EqualExtraValue)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MiniIcon(
                                        res = if (result.isCompleted) Res.drawable.img_state_loading else Res.drawable.img_state_network_error,
                                        size = ThemeValue.Size.LargeImage
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace * 1.5f)
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
                                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RachelText(text = result.reward.toString(), icon = Icons.Outlined.Diamond)
                                    RachelText(text = result.rank.toString(), icon = Icons.Outlined.FormatListNumbered)
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
            modifier = Modifier.fillMaxSize().padding(ThemeValue.Padding.EqualExtraValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
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
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
        ) {
            Column(
                modifier = Modifier.width(ThemeValue.Size.PanelWidth).fillMaxHeight()
                    .padding(ThemeValue.Padding.EqualExtraValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
            ) {
                GameItem(
                    game = details,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                )
            }
            Column(
                modifier = Modifier.width(ThemeValue.Size.PanelWidth).fillMaxHeight()
                    .padding(ThemeValue.Padding.EqualExtraValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
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
                    val result = ClientAPI.request(
                        route = API.User.Game.VerifyGame,
                        data = API.User.Game.VerifyGame.Request(
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
    override fun SubContent(device: Device) {
        game?.let { details ->
            Box(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
                val deviceType = LocalDevice.current.type

                WebImage(
                    uri = remember(deviceType) { game.type.xyPath(deviceType != Device.Type.PORTRAIT) },
                    key = Local.VERSION,
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