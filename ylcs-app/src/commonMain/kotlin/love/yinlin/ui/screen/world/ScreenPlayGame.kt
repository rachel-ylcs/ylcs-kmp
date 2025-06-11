package love.yinlin.ui.screen.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.LocalDevice
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
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
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.world.game.playGameState

@Stable
class ScreenPlayGame(model: AppModel, val args: Args) : SubScreen<ScreenPlayGame.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game, val gid: Int)

    @Stable
    private enum class Status {
        Preparing, Playing, Settling
    }

    override val title: String = args.type.title

    private var status by mutableStateOf(Status.Preparing)

    private val state = playGameState(args.type, slot)

    private var preflightResult: PreflightResult? by mutableStateOf(null)
    private var gameResult: GameResult? by mutableStateOf(null)

    private val canSubmit by derivedStateOf { status == Status.Playing && preflightResult != null && state.canSubmit }

    private suspend fun preflight() {
        val result = ClientAPI.request(
            route = API.User.Game.PreflightGame,
            data = API.User.Game.PreflightGame.Request(
                token = app.config.userToken,
                gid = args.gid
            )
        )
        when (result) {
            is Data.Success -> {
                preflightResult = result.data
                status = Status.Playing
                state.reset()
            }
            is Data.Error -> slot.tip.error(result.message)
        }
    }

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
        if (status == Status.Playing) {
            ActionSuspend(
                icon = Icons.Outlined.Check,
                enabled = canSubmit
            ) {
                preflightResult?.let { preflight ->
                    val result = ClientAPI.request(
                        route = API.User.Game.VerifyGame,
                        data = API.User.Game.VerifyGame.Request(
                            token = app.config.userToken,
                            gid = args.gid,
                            rid = preflight.rid,
                            answer = state.submitAnswer
                        )
                    )
                    when (result) {
                        is Data.Success -> {
                            gameResult = result.data
                            status = Status.Settling
                            preflightResult = null
                        }
                        is Data.Error -> slot.tip.error(result.message)
                    }
                }
            }
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        Box(modifier = Modifier.fillMaxSize()) {
            val isLandscape = LocalDevice.current.type != Device.Type.PORTRAIT

            WebImage(
                uri = remember(isLandscape) { args.type.xyPath(isLandscape) },
                key = Local.VERSION,
                contentScale = ContentScale.Crop,
                alpha = 0.75f,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )

            Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                when (status) {
                    Status.Preparing -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).fillMaxSize(fraction = 0.5f).zIndex(2f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace, Alignment.CenterVertically)
                        ) {
                            SecondaryLoadingButton(
                                text = "开始",
                                modifier = Modifier.padding(ThemeValue.Padding.VerticalSpace),
                                onClick = { preflight() }
                            )
                        }
                    }
                    Status.Playing -> {
                        preflightResult?.let { result ->
                            Surface(
                                modifier = Modifier
                                    .padding(ThemeValue.Padding.EqualExtraValue)
                                    .fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                shadowElevation = ThemeValue.Shadow.Surface
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(ThemeValue.Padding.EqualExtraValue)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                                ) {
                                    with(state) { Content(result) }
                                }
                            }
                        }
                    }
                    Status.Settling -> {
                        gameResult?.let { result ->
                            Surface(
                                modifier = Modifier
                                    .padding(ThemeValue.Padding.EqualExtraValue)
                                    .fillMaxWidth(),
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
                                    with(state) { Settlement(result) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}