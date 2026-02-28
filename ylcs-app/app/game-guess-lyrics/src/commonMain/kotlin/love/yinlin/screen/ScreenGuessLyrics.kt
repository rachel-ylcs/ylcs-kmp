package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import love.yinlin.app
import love.yinlin.common.GLStatus
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.SecondaryLoadingButton
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.coroutines.Coroutines
import love.yinlin.cs.SocketsConnection
import love.yinlin.cs.sockets.LyricsSockets
import love.yinlin.cs.url
import love.yinlin.data.rachel.game.Game
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.replaceAll
import love.yinlin.extension.timeString
import love.yinlin.extension.toJsonString

@Stable
class ScreenGuessLyrics(private val uid: Int, private val name: String) : Screen() {
    private var currentStatus: GLStatus by mutableRefStateOf(GLStatus.Hall)

    private var isConnected by mutableStateOf(false)
    private val connection = object : SocketsConnection() {
        suspend fun send(data: LyricsSockets.CM) {
            if (!super.send(data.toJsonString())) slot.tip.error("无法连接到服务器")
        }

        override suspend fun onConnect() {
            isConnected = true
            send(LyricsSockets.CM.Login(app.config.userToken, LyricsSockets.PlayerInfo(uid, name)))
        }

        override suspend fun onError(err: Throwable) {
            slot.tip.error("断开连接 ${err.message}")
        }

        override suspend fun onDisconnect() {
            isConnected = false
        }

        override suspend fun onMessage(msg: String) {
            when (val data = catchingNull { msg.parseJsonValue<LyricsSockets.SM>() }) {
                is LyricsSockets.SM.Error -> slot.tip.warning(data.message)
                is LyricsSockets.SM.PlayerList -> players.replaceAll(data.players)
                is LyricsSockets.SM.InviteReceived -> handleInvitation(data.player)
                is LyricsSockets.SM.RefuseInvitation -> {
                    slot.tip.warning("${data.player.name}拒绝了你的对战邀请")
                    currentStatus = GLStatus.Hall
                }
                is LyricsSockets.SM.GamePrepare -> handlePreparing(data.player1, data.player2)
                is LyricsSockets.SM.GameStart -> {
                    require(data.questions.size == LyricsSockets.QUESTION_COUNT)
                    (currentStatus as? GLStatus.Preparing)?.let { status ->
                        handlePlaying(info1 = status.info1, info2 = status.info2, questions = data.questions)
                    }
                }
                is LyricsSockets.SM.AnswerUpdated -> {
                    (currentStatus as? GLStatus.Playing)?.let { status ->
                        currentStatus = status.copy(count1 = data.count1, count2 = data.count2)
                    }
                }
                is LyricsSockets.SM.SendResult -> currentStatus = GLStatus.Settling(data.result1, data.result2)
                null -> {}
            }
        }
    }

    private val players = mutableStateListOf<LyricsSockets.PlayerInfo>()

    private suspend fun openSockets() {
        Coroutines.io {
            connection.connect(LyricsSockets)
        }
    }

    private suspend fun sendInvite(info: LyricsSockets.PlayerInfo) {
        if (currentStatus == GLStatus.Hall && slot.confirm.open(content = "邀请${info.name}对战")) {
            connection.send(LyricsSockets.CM.InvitePlayer(info.uid))
            currentStatus = GLStatus.InviteLoading(info, LyricsSockets.INVITE_TIME)
            launch {
                for (_ in 0 ..< (LyricsSockets.INVITE_TIME / 1000L).toInt()) {
                    delay(1000L)
                    (currentStatus as? GLStatus.InviteLoading)?.let { status ->
                        val time = status.time - 1000L
                        if (time <= 0L) break
                        currentStatus = status.copy(time = time)
                    } ?: break
                }
            }
        }
    }

    private suspend fun onInviteResult(info: LyricsSockets.PlayerInfo, accept: Boolean) {
        if (!accept) currentStatus = GLStatus.Hall
        connection.send(LyricsSockets.CM.InviteResponse(info.uid, accept))
    }

    private fun handleInvitation(info: LyricsSockets.PlayerInfo) {
        // 只有在大厅里才能接受邀请, 其他情况均默认拒绝
        if (currentStatus == GLStatus.Hall) {
            currentStatus = GLStatus.InvitedLoading(info, LyricsSockets.INVITE_TIME)
            launch {
                for (_ in 0 ..< (LyricsSockets.INVITE_TIME / 1000L).toInt()) {
                    delay(1000L)
                    (currentStatus as? GLStatus.InvitedLoading)?.let { status ->
                        val time = status.time - 1000L
                        if (time <= 0L) break
                        currentStatus = status.copy(time = time)
                    } ?: return@launch
                }
                currentStatus = GLStatus.Hall
            }
        }
    }

    private fun handlePreparing(info1: LyricsSockets.PlayerInfo, info2: LyricsSockets.PlayerInfo) {
        currentStatus = GLStatus.Preparing(info1, info2, LyricsSockets.PREPARE_TIME)
        launch {
            for (_ in 0 ..< (LyricsSockets.PREPARE_TIME / 1000L).toInt()) {
                delay(1000L)
                (currentStatus as? GLStatus.Preparing)?.let { status ->
                    val time = status.time - 1000L
                    if (time <= 0L) break
                    currentStatus = status.copy(time = time)
                } ?: break
            }
        }
    }

    private fun handlePlaying(info1: LyricsSockets.PlayerInfo, info2: LyricsSockets.PlayerInfo, questions: List<Pair<String, Int>>) {
        currentStatus = GLStatus.Playing(info1, info2, LyricsSockets.PLAYING_TIME, questions, questions.map { null }, 0, 0)
        launch {
            for (_ in 0 ..< (LyricsSockets.PLAYING_TIME / 1000L).toInt()) {
                delay(1000L)
                (currentStatus as? GLStatus.Playing)?.let { status ->
                    val time = status.time - 1000L
                    if (time <= 0L) break
                    currentStatus = status.copy(time = time)
                } ?: break
            }
        }
    }

    override val title: String = Game.GuessLyrics.title

    override fun onBack() {
        launch {
            if (slot.confirm.open(content = "断开服务器连接")) pop()
        }
    }

    override suspend fun initialize() {
        launch { openSockets() }
    }

    @Composable
    private fun UserItem(
        info: LyricsSockets.PlayerInfo,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: (@Composable ColumnScope.() -> Unit)? = null
    ) {
        Box(modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .condition(onClick != null) { clickable(onClick = onClick) }
                    .padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                WebImage(
                    uri = info.userAvatar.url,
                    key = remember { DateEx.TodayString },
                    circle = true,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
                SimpleEllipsisText(text = info.name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                content?.invoke(this)
            }
        }
    }

    @Composable
    private fun GameResultUserItem(
        result: LyricsSockets.GameResult,
        modifier: Modifier = Modifier,
        isWinner: Boolean
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            WebImage(
                uri = result.userAvatar.url,
                key = remember { DateEx.TodayString },
                circle = true,
                modifier = Modifier.size(Theme.size.image7)
            )
            ThemeContainer(if (isWinner) Theme.color.primary else LocalColor.current) {
                TextIconAdapter(modifier = Modifier.align(Alignment.CenterHorizontally)) { idIcon, idText ->
                    if (isWinner) Icon(icon = Icons2.Rank1, color = Colors.Unspecified, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = result.player.name, style = Theme.typography.v6.bold, modifier = Modifier.idText())
                }
                SimpleEllipsisText(text = "正确率: ${result.count} / ${LyricsSockets.QUESTION_COUNT}")
                SimpleEllipsisText(text = "时间: ${result.duration.timeString}")
            }
        }
    }

    @Composable
    private fun HallLayout() {
        if (players.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.image7),
                contentPadding = Theme.padding.eValue,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
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
                            if (it.uid == uid) slot.tip.warning("不能与自己对战")
                            else launch { sendInvite(it) }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun InviteLoadingLayout(status: GLStatus.InviteLoading) {
        Column(
            modifier = Modifier
                .padding(Theme.padding.eValue9)
                .widthIn(max = Theme.size.cell1)
                .fillMaxWidth()
                .padding(Theme.padding.eValue9),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
        ) {
            UserItem(info = status.info, modifier = Modifier.width(Theme.size.image7))
            SimpleEllipsisText(text = "等待对方回应")
            SimpleEllipsisText(text = status.time.timeString, style = Theme.typography.v4.bold)
        }
    }

    @Composable
    private fun InvitedLoadingLayout(status: GLStatus.InvitedLoading) {
        Column(
            modifier = Modifier
                .padding(Theme.padding.eValue9)
                .widthIn(max = Theme.size.cell1)
                .fillMaxWidth()
                .padding(Theme.padding.eValue9),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
        ) {
            UserItem(info = status.info, modifier = Modifier.width(Theme.size.image7))
            SimpleEllipsisText(text = "是否接受对战")
            SimpleEllipsisText(text = status.time.timeString, style = Theme.typography.v4.bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryLoadingButton(text = "接受", icon = Icons.CheckCircle, onClick = { onInviteResult(status.info, true) })
                SecondaryLoadingButton(text = "拒绝", icon = Icons.Cancel, onClick = { onInviteResult(status.info, false) })
            }
        }
    }

    @Composable
    private fun PreparingLayout(status: GLStatus.Preparing) {
        Column(
            modifier = Modifier
                .padding(Theme.padding.eValue9)
                .widthIn(max = Theme.size.cell1)
                .fillMaxWidth()
                .padding(Theme.padding.eValue9),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserItem(info = status.info1, modifier = Modifier.width(Theme.size.image7))
                UserItem(info = status.info2, modifier = Modifier.width(Theme.size.image7))
            }
            SimpleEllipsisText(text = status.time.timeString, style = Theme.typography.v4.bold)
            SimpleEllipsisText(text = "准备时间")
        }
    }

    @Composable
    private fun GameQuestionLayout(status: GLStatus.Playing, modifier: Modifier = Modifier) {
        var index: Int by rememberValueState(0)

        val inputState = remember(index) { InputState(maxLength = 16) }
        val focusRequester = remember { FocusRequester() }

        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = Icons.KeyboardArrowLeft, tip = "上一题", onClick = {
                if (index > 0) --index
                focusRequester.requestFocus()
            })
            SimpleEllipsisText(
                text = (index + 1).toString(),
                style = Theme.typography.v7.bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Icon(icon = Icons.KeyboardArrowRight, tip = "下一题", onClick = {
                if (index < status.questions.size - 1) ++index
                focusRequester.requestFocus()
            })
        }

        val (question, answerLength) = status.questions[index]
        SimpleEllipsisText(text = "上句: $question")
        SimpleEllipsisText(text = "下句: ${status.answers[index] ?: ""}", color = Theme.color.primary)
        Input(
            state = inputState,
            hint = "[下句${answerLength}字](回车保存)",
            onImeClick = {
                if (inputState.isSafe) {
                    val newAnswers = status.answers.toMutableList()
                    val newAnswer = inputState.text
                    newAnswers[index] = newAnswer
                    currentStatus = status.copy(answers = newAnswers)
                    launch {
                        connection.send(LyricsSockets.CM.SaveAnswer(index, newAnswer))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
    }

    @Composable
    private fun GameLayout(status: GLStatus.Playing) {
        Surface(
            modifier = Modifier
                .padding(Theme.padding.eValue9)
                .widthIn(max = Theme.size.cell1)
                .fillMaxWidth(),
            shape = Theme.shape.v3,
            contentPadding = Theme.padding.eValue9,
            shadowElevation = Theme.shadow.v3
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        UserItem(
                            info = status.info1,
                            modifier = Modifier.width(Theme.size.image7)
                        ) {
                            SimpleEllipsisText(text = "${status.count1} / ${LyricsSockets.QUESTION_COUNT}", color = Theme.color.tertiary)
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                    ) {
                        SimpleEllipsisText(text = status.time.timeString, style = Theme.typography.v4.bold)
                        PrimaryLoadingButton(text = "提交", icon = Icons.Check, onClick = {
                            val blankCount = status.answers.count { it == null }
                            val submit = if (blankCount > 0) slot.confirm.open(content = "还有${blankCount}题未填写, 是否提交?") else true
                            if (submit) {
                                connection.send(LyricsSockets.CM.Submit)
                                currentStatus = GLStatus.Waiting(status.info2)
                            }
                        })
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        UserItem(
                            info = status.info2,
                            modifier = Modifier.width(Theme.size.image7)
                        ) {
                            SimpleEllipsisText(text = "${status.count2} / ${LyricsSockets.QUESTION_COUNT}", color = Theme.color.tertiary)
                        }
                    }
                }
                GameQuestionLayout(status = status, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun WaitingLayout(status: GLStatus.Waiting) {
        Column(
            modifier = Modifier
                .padding(Theme.padding.eValue9)
                .widthIn(max = Theme.size.cell1)
                .fillMaxWidth()
                .padding(Theme.padding.eValue9),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
        ) {
            UserItem(info = status.info, modifier = Modifier.width(Theme.size.image7))
            SimpleEllipsisText(text = "等待对方完成...", style = Theme.typography.v6)
        }
    }

    @Composable
    private fun SettlingLayout(status: GLStatus.Settling) {
        Surface(
            modifier = Modifier
                .padding(Theme.padding.eValue9)
                .widthIn(max = Theme.size.cell1)
                .fillMaxWidth(),
            shape = Theme.shape.v3,
            contentPadding = Theme.padding.eValue9,
            shadowElevation = Theme.shadow.v3
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val win = when {
                        status.result1.count > status.result2.count -> true
                        status.result1.count == status.result2.count -> status.result1.duration <= status.result2.duration
                        else -> false
                    }

                    GameResultUserItem(
                        result = status.result1,
                        modifier = Modifier.weight(1f).padding(Theme.padding.eValue9),
                        isWinner = win
                    )
                    GameResultUserItem(
                        result = status.result2,
                        modifier = Modifier.weight(1f).padding(Theme.padding.eValue9),
                        isWinner = !win
                    )
                }
                PrimaryLoadingButton(text = "返回大厅", icon = Icons.Reply, onClick = {
                    currentStatus = GLStatus.Hall
                    connection.send(LyricsSockets.CM.GetPlayers)
                })
            }
        }
    }


    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val status = currentStatus) {
                is GLStatus.Hall -> HallLayout()
                is GLStatus.InviteLoading -> InviteLoadingLayout(status)
                is GLStatus.InvitedLoading -> InvitedLoadingLayout(status)
                is GLStatus.Preparing -> PreparingLayout(status)
                is GLStatus.Playing -> GameLayout(status)
                is GLStatus.Waiting -> WaitingLayout(status)
                is GLStatus.Settling -> SettlingLayout(status)
            }
        }
    }

    override val fab: FAB = object : FAB() {
        override val action: FABAction? by derivedStateOf {
            val icon = if (isConnected) {
                if (currentStatus == GLStatus.Hall) Icons.Refresh else null
            }
            else Icons.Disconnect
            if (icon != null) {
                FABAction(
                    iconProvider = { icon },
                    onClick = {
                        if (isConnected) {
                            if (currentStatus == GLStatus.Hall) connection.send(LyricsSockets.CM.GetPlayers)
                        }
                        else {
                            players.clear()
                            currentStatus = GLStatus.Hall
                            launch { openSockets() }
                        }
                    }
                )
            } else null
        }
    }
}