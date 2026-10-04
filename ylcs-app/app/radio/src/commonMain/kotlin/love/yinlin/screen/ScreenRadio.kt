package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.zIndex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.download
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.FastFixedText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.radio.Radio
import love.yinlin.data.radio.RadioUser
import love.yinlin.data.radio.RadioUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.replaceAll
import love.yinlin.extension.timeString
import love.yinlin.fs.File
import love.yinlin.media.buildAudioPlayer
import love.yinlin.tpl.radio.RadioAPI
import kotlin.time.Duration.Companion.seconds

@Stable
class ScreenRadio : BasicScreen() {
    private val player = buildAudioPlayer(app.rawContext, ::nextRadio)

    private var isPlaying by mutableStateOf(false)
    private var position by mutableLongStateOf(0L)
    private var duration by mutableLongStateOf(0L)

    private val isPlayingFlow = MutableStateFlow(false)

    private val rachel = RadioUserInfo.Default[0]
    private var radioUser: RadioUser? by mutableRefStateOf(null)
    private var radioPrograms: List<Radio> by mutableRefStateOf([])

    private var localMap = mutableStateSetOf<String>()
    private var currentProgram: Radio? by mutableRefStateOf(null)

    private val Radio.audioPath: File get() = File(app.radioPath, id)

    override suspend fun initialize() {
        val result = supervisorScope {
            [
                async { // 初始化播放器
                    player.init()
                    player.isInit
                },
                async { // 加载本地缓存
                    val radioData = Coroutines.io {
                        app.radioPath.list().filter {
                            it.fileSize() > 1024 * 100 // 至少100KB的文件
                        }.map { it.nameWithoutExtension }
                    }
                    localMap.replaceAll(radioData)
                    true
                },
                async { // 加载电台用户
                    val user = RadioAPI.requestUser(rachel.id) ?: return@async false
                    radioUser = user
                    true
                },
                async { // 加载电台节目
                    val cookie = RadioAPI.generateCookie() ?: return@async false
                    val programs = RadioAPI.requestPrograms(rachel.id, cookie) ?: return@async false
                    radioPrograms = programs
                    programs.isNotEmpty()
                }
            ].awaitAll().fastAll { it }
        }

        if (result) {
            launch {
                isPlayingFlow.collectLatest { value ->
                    isPlaying = value
                    duration = player.duration
                    if (value) {
                        while (this@launch.isActive) {
                            position = player.position
                            duration = player.duration
                            delay(1.seconds)
                        }
                    }
                    else position = player.position
                }
            }
        }
        else slot.tip.error("播放器加载失败")
    }

    override fun uninitialize() {
        player.release()
    }

    private suspend fun playProgram(program: Radio) {
        player.load(program.audioPath, true)
        isPlayingFlow.value = true
        currentProgram = program
    }

    private fun nextRadio() {
        launch {
            val current = currentProgram
            if (current != null) {
                val index = radioPrograms.indexOf(current)
                if (index != -1) {
                    val size = radioPrograms.size
                    var target: Radio = current
                    for (step in 1 ..< size) {
                        val idx = (index + step) % size
                        val program = radioPrograms[idx]
                        if (program.id in localMap) {
                            target = program
                            break
                        }
                    }
                    playProgram(target)
                }
            }
        }
    }

    private suspend fun downloadRadioProgram(program: Radio): Boolean {
        // 先获取 audioUrl
        val cookie = RadioAPI.generateCookie() ?: return false
        val audio = RadioAPI.requestAudio(program.audioId, cookie) ?: return false
        // 再下载
        return program.audioPath.bufferedSink().use { sink ->
            downloadDialog.download(audio.url, sink) { }
        }
    }

    private suspend fun deleteRadioProgram(program: Radio) {
        if (currentProgram == program) {
            player.stop()
            isPlayingFlow.value = false
            currentProgram = null
        }
        localMap -= program.id
        slot.tip.success("删除节目缓存成功")
        program.audioPath.delete()
    }

    @Composable
    private fun MusicProgressLayout(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasDuration by rememberDerivedState { duration != 0L && currentProgram != null }

            Icon(
                icon = if (isPlaying) Icons.Pause else Icons.Play,
                onClick = {
                    if (currentProgram != null) {
                        if (isPlaying) player.pause()
                        else player.play()
                        isPlayingFlow.value = !isPlaying
                    }
                }
            )
            FastFixedText("00:00", { position.timeString })
            Slider(
                value = if (duration == 0L) 0f else position / duration.toFloat(),
                onValueChangeFinished = { newProgress ->
                    launch {
                        player.seekTo((newProgress * duration).toLong())
                        isPlayingFlow.value = true
                    }
                },
                enabled = hasDuration,
                trackHeight = Theme.size.box4,
                trackColor = Colors.Gray3,
                activeColor = Colors.Green5,
                trackShape = Theme.shape.circle,
                showThumb = false,
                modifier = Modifier.weight(1f)
            )
            FastFixedText("00:00", { duration.timeString })
        }
    }

    @Composable
    private fun RadioProgramUserCard(
        modifier: Modifier = Modifier,
        user: RadioUserInfo?,
        time: String?,
        style: TextStyle = Theme.typography.v7,
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WebImage(
                uri = user?.avatar ?: "",
                key = user?.id,
                circle = true,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f)
            )
            SimpleEllipsisText(
                text = user?.name ?: "",
                style = style,
                modifier = Modifier.weight(1f)
            )
            SimpleEllipsisText(
                text = time ?: "",
                style = style,
                color = LocalColorVariant.current
            )
        }
    }

    @Composable
    private fun RadioProgramCard(modifier: Modifier = Modifier, program: Radio) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(Theme.size.image6),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(Theme.shape.v8)
                        .condition(program == currentProgram) {
                            border(Theme.border.v5, Theme.color.primary, Theme.shape.v8)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    WebImage(
                        uri = program.pic,
                        key = program.id,
                        modifier = Modifier.fillMaxSize().zIndex(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(Theme.size.image9)
                            .border(Theme.border.v4, Colors.Black, Theme.shape.circle)
                            .background(Colors.White, Theme.shape.circle)
                            .zIndex(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon = if (program.id in localMap) Icons.PlayArrow else Icons.Download, color = Colors.Dark)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    SimpleEllipsisText(
                        text = program.title,
                        color = if (currentProgram == program) Theme.color.primary else LocalColor.current,
                        style = Theme.typography.v7.bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    RadioProgramUserCard(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        user = program.user,
                        time = DateEx.Formatter.standardDate.format(program.time.date),
                        style = Theme.typography.v8
                    )
                    Text(
                        text = program.content,
                        style = Theme.typography.v8,
                        color = LocalColorVariant.current,
                        modifier = Modifier.fillMaxHeight(),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                maxItemsInEachRow = 4
            ) {
                (val commentNum, val likeNum, val repostNum, val playNum) = program.data

                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Commit, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = likeNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Star, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = commentNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = repostNum.toString(), modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Share, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = playNum.toString(), modifier = Modifier.idText())
                }
            }
        }
    }

    private val radioInfoLayout = movableComposable { _: ColumnScope ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverShape = Theme.shape.v7
            Box(modifier = Modifier.size(Theme.size.image5).clip(coverShape).border(
                width = Theme.border.v5,
                color = Theme.color.outline,
                shape = coverShape
            )) {
                val cover = radioUser?.cover
                if (cover != null) LocalFileImage(uri = cover, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText(
                    text = radioUser?.name ?: "搜索电台中...",
                    style = Theme.typography.v6.bold
                )

                RadioProgramUserCard(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    user = radioUser?.user,
                    time = radioUser?.updateTime?.date?.let(DateEx.Formatter.standardDate::format),
                    style = Theme.typography.v7
                )

                Text(
                    text = radioUser?.description ?: "",
                    style = Theme.typography.v8,
                    color = LocalColorVariant.current,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
            maxItemsInEachRow = 4
        ) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Commit, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = (radioUser?.programNum ?: 0).toString(), modifier = Modifier.idText())
            }
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Star, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = (radioUser?.subscriberNum ?: 0).toString(), modifier = Modifier.idText())
            }
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = (radioUser?.commentNum ?: 0).toString(), modifier = Modifier.idText())
            }
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Share, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = (radioUser?.repostNum ?: 0).toString(), modifier = Modifier.idText())
            }
        }

        MusicProgressLayout(modifier = Modifier.fillMaxWidth())
    }

    private val radioListLayout = movableComposable { modifier: Modifier ->
        LazyColumn(modifier = modifier) {
            items(items = radioPrograms, key = { it.id }) { program ->
                RadioProgramCard(
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = {
                            launch {
                                if (program.id in localMap) playProgram(program)
                                else {
                                    if (slot.confirm.open("下载该电台节目吗?", title = "时长: ${program.duration.timeString}")) {
                                        val result = Coroutines.io {
                                            downloadRadioProgram(program)
                                        }
                                        if (result) localMap += program.id
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            launch {
                                if (slot.confirm.open("删除该电台节目吗?")) deleteRadioProgram(program)
                            }
                        }
                    ).padding(Theme.padding.value),
                    program = program
                )
            }
        }
    }

    @Composable
    override fun BasicContent() {
        Box(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize().padding(Theme.padding.eValue9)) {
            val deviceType by rememberDeviceType()

            when (deviceType) {
                Device.Type.PORTRAIT, Device.Type.SQUARE -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                    ) {
                        radioInfoLayout(this)
                        Divider()
                        radioListLayout(Modifier.fillMaxWidth().weight(1f))
                    }
                }
                Device.Type.LANDSCAPE -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9)
                    ) {
                        Column(
                            modifier = Modifier.width(Theme.size.cell1 * 1.25f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                        ) {
                            radioInfoLayout(this)
                        }
                        Divider()
                        radioListLayout(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }

    private val downloadDialog = this land DialogDownload()
}