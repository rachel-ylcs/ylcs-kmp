package love.yinlin.ui.screen.world.online

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
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
import kotlinx.coroutines.delay
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
import love.yinlin.extension.rememberValueState
import love.yinlin.extension.replaceAll
import love.yinlin.extension.timeString
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.LoadingRachelButton
import love.yinlin.ui.component.input.PrimaryLoadingButton
import love.yinlin.ui.component.input.SecondaryLoadingButton
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.layout.SplitLayout
import love.yinlin.ui.component.node.condition
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState

@Composable
private fun UserItem(
    info: LyricsSockets.PlayerInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .condition(onClick != null) { clickable { onClick?.invoke() } }
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
            content?.invoke()
        }
    }
}

@Stable
class ScreenGuessLyrics(model: AppModel, val args: Args) : SubScreen<ScreenGuessLyrics.Args>(model) {
    @Serializable
    data class Args(val uid: Int, val name: String)

    @Stable
    private sealed interface Status {
        @Stable
        data object Hall : Status
        @Stable
        data class InviteLoading(val info: LyricsSockets.PlayerInfo, val time: Long) : Status
        @Stable
        data class InvitedLoading(val info: LyricsSockets.PlayerInfo, val time: Long) : Status
        @Stable
        data class Preparing(val info1: LyricsSockets.PlayerInfo, val info2: LyricsSockets.PlayerInfo, val time: Long) : Status
        @Stable
        data class Playing(val info1: LyricsSockets.PlayerInfo, val info2: LyricsSockets.PlayerInfo, val time: Long, val questions: List<String>, val answers: List<String?>, val count1: Int, val count2: Int) : Status
        @Stable
        data class Waiting(val info: LyricsSockets.PlayerInfo) : Status
        @Stable
        data class Settling(val result1: LyricsSockets.GameResult, val result2: LyricsSockets.GameResult) : Status
    }

    private var currentStatus: Status by mutableStateOf(Status.Hall)
    private var session: DefaultClientWebSocketSession? = null
    private val players = mutableStateListOf<LyricsSockets.PlayerInfo>()

    private suspend inline fun send(data: LyricsSockets.CM) {
        session?.sendSerialized(data) ?: slot.tip.error("无法连接到服务器")
    }

    private suspend fun sendInvite(info: LyricsSockets.PlayerInfo) {
        if (currentStatus == Status.Hall && slot.confirm.openSuspend(content = "邀请${info.name}对战")) {
            send(LyricsSockets.CM.InvitePlayer(info.uid))
            currentStatus = Status.InviteLoading(info, LyricsSockets.INVITE_TIME)
            launch {
                for (i in 0 ..< (LyricsSockets.INVITE_TIME / 1000L).toInt()) {
                    delay(1000L)
                    (currentStatus as? Status.InviteLoading)?.let { status ->
                        val time = status.time - 1000L
                        if (time <= 0L) break
                        currentStatus = status.copy(time = time)
                    } ?: break
                }
            }
        }
    }

    private suspend fun onInviteResult(info: LyricsSockets.PlayerInfo, accept: Boolean) {
        if (!accept) currentStatus = Status.Hall
        send(LyricsSockets.CM.InviteResponse(info.uid, accept))
    }

    private fun handleInvitation(info: LyricsSockets.PlayerInfo) {
        // 只有在大厅里才能接受邀请, 其他情况均默认拒绝
        if (currentStatus == Status.Hall) {
            currentStatus = Status.InvitedLoading(info, LyricsSockets.INVITE_TIME)
            launch {
                for (i in 0 ..< (LyricsSockets.INVITE_TIME / 1000L).toInt()) {
                    delay(1000L)
                    (currentStatus as? Status.InvitedLoading)?.let { status ->
                        val time = status.time - 1000L
                        if (time <= 0L) break
                        currentStatus = status.copy(time = time)
                    } ?: return@launch
                }
                currentStatus = Status.Hall
            }
        }
    }

    private fun handlePreparing(info1: LyricsSockets.PlayerInfo, info2: LyricsSockets.PlayerInfo) {
        currentStatus = Status.Preparing(info1, info2, LyricsSockets.PREPARE_TIME)
        launch {
            for (i in 0 ..< (LyricsSockets.INVITE_TIME / 1000L).toInt()) {
                delay(1000L)
                (currentStatus as? Status.Preparing)?.let { status ->
                    val time = status.time - 1000L
                    if (time <= 0L) break
                    currentStatus = status.copy(time = time)
                } ?: break
            }
        }
    }

    private fun handlePlaying(info1: LyricsSockets.PlayerInfo, info2: LyricsSockets.PlayerInfo, questions: List<String>) {
        currentStatus = Status.Playing(info1, info2, LyricsSockets.PLAYING_TIME, questions, questions.map { null }, 0, 0)
        launch {
            for (i in 0 ..< (LyricsSockets.INVITE_TIME / 1000L).toInt()) {
                delay(1000L)
                (currentStatus as? Status.Playing)?.let { status ->
                    val time = status.time - 1000L
                    if (time <= 0L) break
                    currentStatus = status.copy(time = time)
                } ?: break
            }
        }
    }

    private fun dispatchMessage(msg: LyricsSockets.SM) {
        when (msg) {
            is LyricsSockets.SM.Error -> slot.tip.warning(msg.message)
            is LyricsSockets.SM.PlayerList -> players.replaceAll(msg.players)
            is LyricsSockets.SM.InviteReceived -> handleInvitation(msg.player)
            is LyricsSockets.SM.RefuseInvitation -> {
                slot.tip.warning("${msg.player.name}拒绝了你的对战邀请")
                currentStatus = Status.Hall
            }
            is LyricsSockets.SM.GamePrepare -> handlePreparing(msg.player1, msg.player2)
            is LyricsSockets.SM.GameStart -> {
                require(msg.questions.size == LyricsSockets.QUESTION_COUNT)
                (currentStatus as? Status.Preparing)?.let { status ->
                    handlePlaying(info1 = status.info1, info2 = status.info2, questions = msg.questions)
                }
            }
            is LyricsSockets.SM.AnswerUpdated -> {
                (currentStatus as? Status.Playing)?.let { status ->
                    currentStatus = status.copy(count1 = msg.count1, count2 = msg.count2)
                }
            }
            is LyricsSockets.SM.SendResult -> currentStatus = Status.Settling(msg.result1, msg.result2)
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
                            else launch { sendInvite(it) }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun InviteLoadingLayout(status: Status.InviteLoading) {
        Column(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxWidth()
                .padding(ThemeValue.Padding.EqualExtraValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            UserItem(info = status.info, modifier = Modifier.width(ThemeValue.Size.CellWidth))
            Space(ThemeValue.Padding.VerticalExtraSpace)
            Text(
                text = "等待对方回应",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Space(ThemeValue.Padding.VerticalExtraSpace)
            Text(
                text = remember(status) { status.time.timeString },
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun InvitedLoadingLayout(status: Status.InvitedLoading) {
        Column(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxWidth()
                .padding(ThemeValue.Padding.EqualExtraValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            UserItem(info = status.info, modifier = Modifier.width(ThemeValue.Size.CellWidth))
            Space(ThemeValue.Padding.VerticalExtraSpace)
            Text(
                text = "是否接受对战",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Space(ThemeValue.Padding.VerticalExtraSpace)
            Text(
                text = remember(status) { status.time.timeString },
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Space(ThemeValue.Padding.VerticalExtraSpace)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryLoadingButton(
                    text = "接受",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = { onInviteResult(status.info, true) }
                )
                SecondaryLoadingButton(
                    text = "拒绝",
                    icon = Icons.Outlined.Cancel,
                    onClick = { onInviteResult(status.info, false) }
                )
            }
        }
    }

    @Composable
    private fun PreparingLayout(status: Status.Preparing) {
        Column(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxWidth()
                .padding(ThemeValue.Padding.EqualExtraValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            SplitLayout(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = ThemeValue.Padding.HorizontalExtraSpace,
                verticalAlignment = Alignment.CenterVertically,
                left = {
                    UserItem(info = status.info1, modifier = Modifier.width(ThemeValue.Size.LargeImage))
                },
                right = {
                    UserItem(info = status.info2, modifier = Modifier.width(ThemeValue.Size.LargeImage))
                }
            )
            Text(
                text = remember(status) { status.time.timeString },
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "准备时间",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun ColumnScope.GameQuestionLayout(
        status: Status.Playing,
        modifier: Modifier = Modifier,
    ) {
        var index: Int by rememberValueState(0)

        val inputState = remember(index) { TextInputState() }

        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickIcon(
                icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                onClick = {
                    if (index > 0) --index
                }
            )
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ClickIcon(
                icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                onClick = {
                    if (index < status.questions.size - 1) ++index
                }
            )
        }

        Text(
            text = status.questions[index],
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = status.answers[index] ?: "",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = inputState,
            hint = "输入歌词下句(回车保存)",
            clearButton = false,
            maxLength = 16,
            onImeClick = {
                if (inputState.ok) {
                    val newAnswers = status.answers.toMutableList()
                    val newAnswer = inputState.text
                    newAnswers[index] = newAnswer
                    currentStatus = status.copy(answers = newAnswers)
                    launch {
                        send(LyricsSockets.CM.SaveAnswer(index, newAnswer))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun GameLayout(status: Status.Playing) {
        Surface(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = ThemeValue.Shadow.Surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(ThemeValue.Padding.EqualExtraValue)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        UserItem(
                            info = status.info1,
                            modifier = Modifier.width(ThemeValue.Size.LargeImage)
                        ) {
                            Text(text = status.count1.toString())
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
                    ) {
                        Text(
                            text = remember(status) { status.time.timeString },
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        PrimaryLoadingButton(
                            text = "提交",
                            icon = Icons.Outlined.Check,
                            onClick = {
                                send(LyricsSockets.CM.Submit)
                                currentStatus = Status.Waiting(status.info2)
                            }
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        UserItem(
                            info = status.info2,
                            modifier = Modifier.width(ThemeValue.Size.LargeImage)
                        ) {
                            Text(text = status.count2.toString())
                        }
                    }
                }
                GameQuestionLayout(
                    status = status,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun WaitingLayout(status: Status.Waiting) {
        Column(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxWidth()
                .padding(ThemeValue.Padding.EqualExtraValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            UserItem(info = status.info, modifier = Modifier.width(ThemeValue.Size.CellWidth))
            Space(ThemeValue.Padding.VerticalExtraSpace)
            Text(
                text = "等待对方完成...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Space(ThemeValue.Padding.VerticalExtraSpace)
        }
    }

    @Composable
    private fun SettlingLayout(status: Status.Settling) {
        Surface(
            modifier = Modifier
                .padding(ThemeValue.Padding.EqualExtraValue)
                .widthIn(max = ThemeValue.Size.PanelWidth)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = ThemeValue.Shadow.Surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(ThemeValue.Padding.EqualExtraValue)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
            ) {
                Text(text = status.result1.toString())
                Text(text = status.result2.toString())
            }
        }
    }

    override val title: String = Game.GuessLyrics.title

    override fun onBack() {
        launch {
            if (slot.confirm.openSuspend(content = "断开服务器连接")) pop()
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
            when (val status = currentStatus) {
                is Status.Hall -> HallLayout()
                is Status.InviteLoading -> InviteLoadingLayout(status)
                is Status.InvitedLoading -> InvitedLoadingLayout(status)
                is Status.Preparing -> PreparingLayout(status)
                is Status.Playing -> GameLayout(status)
                is Status.Waiting -> WaitingLayout(status)
                is Status.Settling -> SettlingLayout(status)
            }
        }
    }

    override val fabIcon: ImageVector? by derivedStateOf { if (currentStatus == Status.Hall) Icons.Outlined.Refresh else null }

    override suspend fun onFabClick() {
        if (currentStatus == Status.Hall) send(LyricsSockets.CM.GetPlayers)
    }
}