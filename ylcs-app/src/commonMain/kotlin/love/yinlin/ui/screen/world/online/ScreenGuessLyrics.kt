package love.yinlin.ui.screen.world.online

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.sockets.LyricsSockets
import love.yinlin.extension.DateEx
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.replaceAll
import love.yinlin.platform.app
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen

@Composable
private fun UserItem(
    info: LyricsSockets.PlayerInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(ThemeValue.Padding.EqualExtraValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            WebImage(
                uri = info.avatarPath,
                key = remember { DateEx.TodayString },
                circle = true,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
            Text(
                text = info.name,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Stable
class ScreenGuessLyrics(model: AppModel, val args: Args) : SubScreen<ScreenGuessLyrics.Args>(model) {
    @Serializable
    data class Args(val uid: Int, val name: String)

    @Stable
    private enum class Status {
        Hall, Playing, Settling
    }

    private var status by mutableStateOf(Status.Hall)
    private var session: DefaultClientWebSocketSession? = null
    private val players = mutableStateListOf<LyricsSockets.PlayerInfo>()

    override val title: String = Game.GuessLyrics.title

    private suspend inline fun send(data: LyricsSockets.CM) {
        session?.sendSerialized(data)
    }

    private suspend fun dispatchMessage(msg: LyricsSockets.SM) {
        when (msg) {
            is LyricsSockets.SM.Error -> slot.tip.warning(msg.message)
            is LyricsSockets.SM.GameStart -> {

            }
            is LyricsSockets.SM.InviteReceived -> {

            }
            is LyricsSockets.SM.InviteResult -> {

            }
            is LyricsSockets.SM.OtherAnswerUpdated -> {

            }
            is LyricsSockets.SM.PlayerList -> players.replaceAll(msg.players)
            is LyricsSockets.SM.SendResult -> {

            }
        }
    }

    @Composable
    private fun HallLayout() {
        if (players.isEmpty()) EmptyBox()
        else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(ThemeValue.Size.LargeImage),
                contentPadding = ThemeValue.Padding.EqualValue,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = players,
                    key = { it.uid }
                ) {
                    UserItem(
                        info = it,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (it.uid == args.uid) slot.tip.warning("不能与自己对战")
                            else launch { send(LyricsSockets.CM.InvitePlayer(it.uid)) }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun GameLayout() {
        Surface(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = ThemeValue.Shadow.Surface
        ) {

        }
    }

    @Composable
    private fun ResultLayout() {
        Surface(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = ThemeValue.Shadow.Surface
        ) {

        }
    }

    override suspend fun initialize() {
        launch {
            try {
                val newSession = app.socketsClient.webSocketSession(host = Local.API_HOST, path = LyricsSockets.path)
                session = newSession
                send(LyricsSockets.CM.Login(LyricsSockets.PlayerInfo(args.uid, args.name)))
                newSession.incoming.consumeAsFlow().collect { frame ->
                    if (frame is Frame.Text) {
                        val msg = frame.readText().parseJsonValue<LyricsSockets.SM>()
                        if (msg != null) dispatchMessage(msg)
                    }
                }
            }
            catch (_: Throwable) {
                slot.tip.error("无法连接到服务器")
            } finally {
                session?.close()
                session = null
            }
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        Box(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                Status.Hall -> HallLayout()
                Status.Playing -> GameLayout()
                Status.Settling -> ResultLayout()
            }
        }
    }

    override val fabIcon: ImageVector? by derivedStateOf { if (status == Status.Hall) Icons.Outlined.Refresh else null }

    override suspend fun onFabClick() {
        if (status == Status.Hall) send(LyricsSockets.CM.GetPlayers)
    }
}