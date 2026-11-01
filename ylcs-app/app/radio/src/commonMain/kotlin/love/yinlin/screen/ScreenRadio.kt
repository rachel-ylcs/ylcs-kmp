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
import androidx.compose.ui.zIndex
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import kotlinx.io.writeString
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
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.download
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
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
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.replaceAll
import love.yinlin.extension.timeString
import love.yinlin.extension.toJsonString
import love.yinlin.fs.File
import love.yinlin.media.buildAudioPlayer
import love.yinlin.tpl.radio.RadioAPI
import kotlin.time.Duration.Companion.seconds

@Stable
class ScreenRadio : Screen() {
    private val player = buildAudioPlayer(app.rawContext, ::nextRadio)

    private var isPlaying by mutableStateOf(false)
    private var isPlayerReady by mutableStateOf(false)
    private var position by mutableLongStateOf(0L)
    private var duration by mutableLongStateOf(0L)

    private val isPlayingFlow = MutableStateFlow(false)

    private val rachel = RadioUserInfo.Default[0]
    private var radioUser: RadioUser? by mutableRefStateOf(null)
    private var radioPrograms: List<Radio> by mutableRefStateOf([])
    private var isUpdating by mutableStateOf(true)
    private var canDownload by mutableStateOf(false)

    private val radioVersionKey: Int get() = radioPrograms.size

    private var localMap = mutableStateSetOf<String>()
    private var currentProgram: Radio? by mutableRefStateOf(null)

    private val Radio.audioPath: File get() = File(app.radioPath, id)

    override val title: String = "电台"

    override fun initialize() {
        launch {
            isPlayerReady = catchingDefault(false) {
                player.init()
                player.isInit
            }
            if (isPlayerReady) {
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
            else slot.tip.error("播放器加载失败")
        }

        launch {
            // 先展示本地信息，播放器与网络更新互不依赖
            loadRadioInfo()?.let { [user, programs] ->
                radioUser = user
                radioPrograms = programs
            }
            val radioData = Coroutines.io {
                catchingDefault([]) {
                    app.radioPath.list().filter {
                        it.name.isNotEmpty() && it.name.all { char -> char in '0' .. '9' } && it.fileSize() > 1024 * 100 // 至少100KB的音频文件
                    }.map { it.name }
                }
            }
            localMap.replaceAll(radioData)

            val [user, programs] = supervisorScope {
                val user = async { RadioAPI.requestUser(rachel.id) }
                val programs = async {
                    val cookie = RadioAPI.generateCookie() ?: return@async null
                    RadioAPI.requestPrograms(rachel.id, cookie)
                }
                user.await() to programs.await()
            }
            isUpdating = false
            if (user == null || programs == null || !isValidRadioInfo(user, programs)) {
                slot.tip.warning("无法更新电台信息，仅可播放或删除已缓存节目")
                return@launch
            }

            if (radioUser == null || programs.size > radioVersionKey) {
                if (!saveRadioInfo(user, programs)) slot.tip.warning("电台信息缓存失败")
                radioUser = user
                radioPrograms = programs
            }
            canDownload = true
        }
    }

    override fun uninitialize() {
        player.release()
    }

    private fun isValidRadioInfo(user: RadioUser, programs: List<Radio>): Boolean {
        val ids = programs.map { it.id }
        return user.id == rachel.id && user.programNum == programs.size &&
            ids.all { id -> id.isNotEmpty() && id.all { it in '0' .. '9' } } && ids.toSet().size == programs.size
    }

    private suspend fun loadRadioInfo(): Pair<RadioUser, List<Radio>>? = Coroutines.io {
        catchingNull {
            val user = File(app.radioPath, "user.json").readText()!!.parseJsonValue<RadioUser>()
            val programs = File(app.radioPath, "list.json").readText()!!.parseJsonValue<List<Radio>>()
            require(isValidRadioInfo(user, programs))
            user to programs
        }
    }

    private suspend fun saveRadioInfo(user: RadioUser, programs: List<Radio>): Boolean = Coroutines.io {
        catchingDefault(false) {
            app.radioPath.mkdir()
            File(app.radioPath, "user.json").write { it.writeString(user.toJsonString()) }
            File(app.radioPath, "list.json").write { it.writeString(programs.toJsonString()) }
            true
        }
    }

    private suspend fun playProgram(program: Radio) {
        if (!isPlayerReady) return
        player.load(program.audioPath, true)
        isPlayingFlow.value = true
        currentProgram = program
    }

    private fun nextRadio() {
        launch {
            val current = currentProgram
            if (current != null) {
                val index = radioPrograms.indexOfFirst { it.id == current.id }
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
        if (!canDownload) return false
        val path = program.audioPath
        var downloaded = false
        cleaning({
            downloaded = catchingDefault(false) {
                // 先获取 audioUrl
                val cookie = RadioAPI.generateCookie() ?: return@catchingDefault false
                val audio = RadioAPI.requestAudio(program.audioId, cookie) ?: return@catchingDefault false
                // 再下载，输出流关闭后才标记成功
                path.bufferedSink().use { sink ->
                    downloadDialog.download(audio.url, sink) { }
                }
            }
            return downloaded
        }) {
            if (!downloaded) Coroutines.wait { path.delete() }
        }
    }

    private suspend fun deleteRadioProgram(program: Radio) {
        if (program.id !in localMap) return
        if (currentProgram?.id == program.id) {
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
                enabled = isPlayerReady && currentProgram != null,
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
                source = user?.avatar,
                version = user?.id,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                options = ImageOptions.Circular
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
                        .condition(program.id == currentProgram?.id) {
                            border(Theme.border.v5, Theme.color.primary, Theme.shape.v8)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    WebImage(
                        source = program.pic,
                        version = program.id,
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
                        val cached = program.id in localMap
                        Icon(
                            icon = if (cached) Icons.PlayArrow else Icons.Download,
                            color = Colors.Dark,
                            enabled = if (cached) isPlayerReady else canDownload
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    SimpleEllipsisText(
                        text = program.title,
                        color = when {
                            currentProgram?.id == program.id -> Theme.color.primary
                            program.id in localMap -> Theme.color.secondary
                            else -> LocalColor.current
                        },
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
                if (cover != null) {
                    WebImage(
                        source = cover,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText(
                    text = radioUser?.name ?: if (isUpdating) "搜索电台中..." else "暂无电台信息",
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
                        enabled = program.id in localMap || canDownload,
                        onClick = {
                            launch {
                                if (program.id in localMap) playProgram(program)
                                else if (canDownload) {
                                    if (slot.confirm.open("下载该电台节目吗?", title = "时长: ${program.duration.timeString}")) {
                                        val result = Coroutines.io {
                                            downloadRadioProgram(program)
                                        }
                                        if (result) localMap += program.id
                                    }
                                }
                            }
                        },
                        onLongClick = if (program.id in localMap) {
                            {
                                launch {
                                    if (slot.confirm.open("删除该电台节目吗?")) deleteRadioProgram(program)
                                }
                            }
                        }
                        else null
                    ).padding(Theme.padding.value),
                    program = program
                )
            }
        }
    }

    @Composable
    override fun Content() {
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
