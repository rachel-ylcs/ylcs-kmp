package love.yinlin.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import love.yinlin.app
import love.yinlin.app.music.resources.Res
import love.yinlin.app.music.resources.img_music_record
import love.yinlin.compose.*
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.compose.extension.*
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.animation.WaveLoading
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.Stepper
import love.yinlin.compose.ui.input.StepperState
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.layout.Grid
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.node.*
import love.yinlin.compose.ui.text.FastFixedText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.text.TextIconBinder
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.cleaning
import love.yinlin.extension.then
import love.yinlin.extension.timeString
import love.yinlin.startup.StartupMusicPlayer

@Stable
class ScreenMusic : BasicScreen() {
    private val musicPlayer by derivedStateOf { app.requireClassOrNull<StartupMusicPlayer>() }

    private fun requirePlayer(): StartupMusicPlayer? {
        val player = musicPlayer
        if (player != null && player.isInit) return player
        return null
    }

    private val blurState = BlurState()

    @Composable
    private fun MusicCoverLayout(musicInfo: MusicInfo?, modifier: Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(res = Res.drawable.img_music_record, modifier = Modifier.fillMaxSize().zIndex(1f))

            AnimationContent(
                state = musicInfo,
                duration = Theme.animation.duration.v1,
                enter = { fadeIn(animationSpec = tween(it)) },
                exit = { fadeOut(animationSpec = tween(it, delayMillis = it / 2)) },
                modifier = Modifier.fillMaxSize(fraction = 0.7f).fastClipCircle().zIndex(2f)
            ) { info ->
                if (info != null) {
                    LocalFileImage(
                        source = info.path(app.modPath, ModResourceType.Record),
                        version = "${info.id}-${info.modification}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @Composable
    private fun ResourceActiveIcon(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
        TextIconBinder(modifier = Modifier.clickable(onClick = onClick)) { idIcon, idText ->
            Icon(icon = icon, color = color, modifier = Modifier.idIcon())
            SimpleEllipsisText(text = text, color = color, modifier = Modifier.idText())
        }
    }

    @Composable
    private fun FeatureTitle(text: String, icon: ImageVector, modifier: Modifier = Modifier, color: Color = LocalColor.current) {
        TextIconAdapter(modifier = modifier) { idIcon, idText ->
            Icon(icon = icon, color = color, modifier = Modifier.idIcon())
            SimpleEllipsisText(text = text, color = color, style = Theme.typography.v7.bold, modifier = Modifier.idText())
        }
    }

    @Composable
    private fun ToolLayout(player: StartupMusicPlayer, musicInfo: MusicInfo?, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicCoverLayout(musicInfo, modifier = Modifier.size(Theme.size.image7))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                AnimationContent(state = musicInfo?.name, modifier = Modifier.fillMaxWidth()) {
                    Text(text = it ?: "无音源", color = Theme.color.onSurface, style = Theme.typography.v6.bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                AnimationContent(state = musicInfo?.singer, modifier = Modifier.fillMaxWidth()) {
                    SimpleEllipsisText(text = it ?: "未知歌手", color = Theme.color.onSurfaceVariant, style = Theme.typography.v8)
                }
            }

            Row(
                modifier = Modifier.padding(end = Theme.padding.h),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (musicInfo != null) {
                    if (player.currentHasAnimation) {
                        ResourceActiveIcon(
                            text = "动画",
                            icon = Icons.GifBox,
                            color = if (player.currentUseAnimationBackground) Theme.color.primary else Theme.color.onSurfaceVariant,
                            onClick = { player.currentUseAnimationBackground = !player.currentUseAnimationBackground }
                        )
                    }

                    if (player.currentHasAccompaniment) {
                        ResourceActiveIcon(
                            text = "伴奏",
                            icon = Icons.MusicNote,
                            color = Theme.color.primary,
                            onClick = {
                                launch {
                                    player.pause()
                                    navigate(::ScreenAccompaniment, musicInfo, player.engine.type)
                                }
                            }
                        )
                    }

                    if (player.currentHasVideo) {
                        ResourceActiveIcon(
                            text = "视频",
                            icon = Icons.MusicVideo,
                            color = Theme.color.primary,
                            onClick = {
                                launch {
                                    player.pause()
                                    navigate(::ScreenVideo, musicInfo.path(app.modPath, ModResourceType.Video).path)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private val randomLibraryCovers by derivedStateOf { DataSourceMusic.library.values.shuffled().take(4) }

    private val sleepHour = StepperState(min = 0, max = 11)
    private val sleepMinutes = StepperState(min = 0, max = 59)

    private val featureLayout = movableComposable { player: StartupMusicPlayer, modifier: Modifier ->
        val padding = Theme.padding.e
        val shape = Theme.shape.v5
        val backgroundColor = Theme.color.onSurface.copy(alpha = 0.06f)
        val borderColor = Theme.color.onSurface.copy(alpha = 0.12f)
        val borderWidth = Theme.border.v10

        Grid(
            builder = {
                x(0.25f, 0.25f, 0.25f, 0.25f)
                y(max, max, auto, max)
                s(padding)
            },
            modifier = modifier
        ) {
            // 曲库
            Column(
                modifier = Modifier.fillMaxWidth().clip(shape)
                    .background(backgroundColor)
                    .border(borderWidth, borderColor, shape)
                    .clickable { navigate(::ScreenMusicLibrary) }
                    .padding(Theme.padding.value9)
                    .item(xs = 2, ys = 2),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
            ) {
                FeatureTitle(text = "曲库", icon = Icons.LibraryMusic)

                Box(modifier = Modifier.fillMaxWidth()) {
                    for (index in randomLibraryCovers.indices.reversed()) {
                        val music = randomLibraryCovers[index]

                        LocalFileImage(
                            source = music.path(app.modPath, ModResourceType.Record),
                            version = music.modification.toString(),
                            contentScale = ContentScale.Crop,
                            alpha = (0.95f - index * 0.1f).coerceIn(0f, 1f),
                            modifier = Modifier.offset(x = Theme.padding.h8 * index).size(Theme.size.image8).clip(Theme.shape.v7)
                        )
                    }
                }
            }

            // 歌单
            Box(
                modifier = Modifier.fillMaxSize().clip(shape)
                    .background(backgroundColor)
                    .border(borderWidth, borderColor, shape)
                    .clickable { navigate(::ScreenPlaylistLibrary) }
                    .padding(Theme.padding.value9)
                    .item(xs = 2),
                contentAlignment = Alignment.CenterStart
            ) {
                FeatureTitle(text = "歌单", icon = Icons.QueueMusic)
            }

            // 歌词
            Box(
                modifier = Modifier.fillMaxSize().clip(shape)
                    .background(backgroundColor)
                    .border(borderWidth, borderColor, shape)
                    .clickable { navigate(::ScreenLyricsSettings) }
                    .padding(Theme.padding.value9)
                    .item(xs = 2),
                contentAlignment = Alignment.CenterStart
            ) {
                FeatureTitle(text = "歌词", icon = Icons.Lyrics)
            }

            // 工坊
            Column(
                modifier = Modifier.fillMaxWidth().clip(shape)
                    .background(backgroundColor)
                    .border(borderWidth, borderColor, shape)
                    .padding(Theme.padding.value9)
                    .item(xs = 4),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
            ) {
                FeatureTitle(text = "工坊", icon = Icons.Token)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    FeatureTitle(
                        text = "合集",
                        icon = Icons.LibraryMusic,
                        color = Theme.color.tertiary,
                        modifier = Modifier.clickable { navigate(::ScreenModCenter) }.padding(Theme.padding.value)
                    )
                    FeatureTitle(
                        text = "平台",
                        icon = Icons.CloudDownload,
                        color = Theme.color.tertiary.copy(alpha = 0.9f),
                        modifier = Modifier.clickable { navigate(::ScreenPlatformMusic, null, null) }.padding(Theme.padding.value)
                    )
                    FeatureTitle(
                        text = "导入",
                        icon = Icons.Upload,
                        color = Theme.color.tertiary.copy(alpha = 0.8f),
                        modifier = Modifier.clickable { navigate(::ScreenImportMusic, null) }.padding(Theme.padding.value)
                    )
                    FeatureTitle(
                        text = "创建",
                        icon = Icons.DesignServices,
                        color = Theme.color.tertiary.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { navigate(::ScreenCreateMusic) }.padding(Theme.padding.value)
                    )
                }
            }

            // 睡眠模式
            Column(
                modifier = Modifier.fillMaxWidth().clip(shape)
                    .background(backgroundColor)
                    .border(borderWidth, borderColor, shape)
                    .padding(Theme.padding.value9)
                    .item(xs = 3),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                val timer = player.sleepTimer

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureTitle(text = "睡眠模式", icon = Icons.AlarmOn)

                    FeatureTitle(
                        text = if (timer.isRunning) "停止" else "启动",
                        icon = if (timer.isRunning) Icons.Clear else Icons.Check,
                        color = if (timer.isRunning) Theme.color.warning else Theme.color.primary,
                        modifier = Modifier.clickable {
                            if (player.isReady) {
                                if (timer.isRunning) timer.stop()
                                else {
                                    val sleepTime = sleepHour.value * 3600 + sleepMinutes.value * 60
                                    if (sleepTime > 0) timer.start(sleepTime)
                                    else slot.tip.warning("未设定时间")
                                }
                            }
                        }
                    )
                }

                if (timer.isRunning) {
                    SimpleEllipsisText(
                        text = (timer.remainSeconds * 1000L).timeString,
                        style = Theme.typography.v5.bold,
                        color = Theme.color.secondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val converter = remember {
                            { value: Int -> value.toString().padStart(2, '0') }
                        }

                        Stepper(state = sleepHour, enabled = player.isReady, faster = true, label = "小时", converter = converter)
                        Stepper(state = sleepMinutes, enabled = player.isReady, faster = true, label = "分钟", converter = converter)
                    }
                }
            }
        }
    }

    private val lyricsLayout = movableComposable { player: StartupMusicPlayer, modifier: Modifier ->
        if (player.isReady) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                player.engine.LyricsCanvas(config = app.config.lyricsEngineConfig, host = player.engineHost)
            }
        }
        else Box(modifier = modifier)
    }

    @Composable
    private fun MusicProgressSlider(player: StartupMusicPlayer, musicInfo: MusicInfo?, modifier: Modifier) {
        var isDragging by rememberFalse()
        var displayTime by rememberValueState(0L)

        LaunchedEffect(Unit) {
            snapshotFlow { player.showCurrentTime }.collectLatest { time ->
                if (!isDragging && displayTime != time) displayTime = time
            }
        }

        Layout(modifier = modifier.pointerInput(musicInfo) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                val width = size.width.toFloat()
                val duration = player.duration
                if (duration == 0L || width < 0f) return@awaitEachGesture

                isDragging = true

                cleaning({
                    displayTime = (down.position.x / width * duration).toLong().fastCoerceIn(0L, duration)
                    var dragChange = down

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed != dragChange.pressed) break

                        if (change.positionChange() != Offset.Zero) {
                            displayTime = (change.position.x / width * duration).toLong().fastCoerceIn(0L, duration)
                            change.consume()
                        }
                        dragChange = change
                    } while (dragChange.pressed)

                    launch {
                        // 检查是否在副歌点附近
                        val seekTime = musicInfo?.chorus?.find { abs(it - displayTime) <= 3000L } ?: displayTime
                        player.seekTo(seekTime)
                    }
                }) {
                    isDragging = false
                }
            }
        }.drawWithContent {
            val duration = musicPlayer?.duration ?: 0L
            val progress = if (duration == 0L) 0f else displayTime / duration.toFloat()
            val width = size.width
            val height = size.height
            val cornerRadius = CornerRadius(height / 2)
            val hotpotRadius = height * 0.75f
            val offsetProgress = width * progress

            // 画底色
            drawRoundRect(color = Colors.Gray4, cornerRadius = cornerRadius)
            // 画进度
            drawRoundRect(color = Colors.Green5, size = Size(offsetProgress, height), cornerRadius = cornerRadius)
            // 画副歌点
            musicInfo?.chorus?.fastForEach { hotpot ->
                val offsetChorus = width * hotpot / duration.toFloat()
                val leftBound = offsetChorus - hotpotRadius
                val rightBound = offsetChorus + hotpotRadius
                val color = when {
                    offsetProgress <= leftBound -> Colors.White
                    offsetProgress >= rightBound -> Colors.Green2
                    else -> lerp(Colors.White, Colors.Green2, (offsetProgress - leftBound) / (rightBound - leftBound))
                }
                drawCircle(color = color, radius = hotpotRadius, center = Offset(offsetChorus, height / 2))
            }
        }, MeasurePolicies.Empty)
    }

    private val musicControlLayout = movableComposable { player: StartupMusicPlayer, modifier: Modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadingIcon(
                icon = when (player.playMode) {
                    MediaPlayMode.Loop -> Icons.LoopMode
                    MediaPlayMode.Random -> Icons.ShuffleMode
                    else -> Icons.OrderMode
                },
                tip = "播放模式",
                onClick = { player.switchPlayMode() }
            )

            LoadingIcon(
                icon = Icons.GotoPrevious,
                color = Theme.color.primary,
                tip = "上一首",
                onClick = { player.gotoPrevious() }
            )

            Box(
                modifier = Modifier.wrapContentSize()
                    .fastClipCircle()
                    .background(Theme.color.secondaryContainer)
                    .clickable {
                        launch {
                            if (player.isPlaying) player.pause()
                            else player.play()
                        }
                    }.padding(Theme.padding.e9),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon = if (player.isPlaying) Icons.Pause else Icons.Play,
                    color = Theme.color.onContainer,
                    modifier = Modifier.fastOffsetX { if (player.isPlaying) null else 1.5.dp.toPx() }
                )
            }

            LoadingIcon(
                icon = Icons.GotoNext,
                color = Theme.color.primary,
                tip = "下一首",
                onClick = { player.gotoNext() }
            )

            Icon(
                icon = Icons.Playlist,
                tip = "当前播放列表",
                onClick = {
                    if (player.isReady) currentPlaylistSheet.open()
                }
            )
        }
    }

    private val musicProgressLayout = movableComposable { player: StartupMusicPlayer, musicInfo: MusicInfo?, modifier: Modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FastFixedText("00:00", { player.showCurrentTime.timeString })
            MusicProgressSlider(player, musicInfo, modifier = Modifier.weight(1f).height(6.dp).pointerIcon(PointerIcon.Hand))
            FastFixedText("00:00", { player.duration.timeString })
        }
    }

    @Composable
    private fun PlaybackLayout(player: StartupMusicPlayer, musicInfo: MusicInfo?, horizontal: Boolean, modifier: Modifier = Modifier) {
        if (horizontal) {
            Row(
                modifier = modifier.clip(Theme.shape.v4).background(Theme.color.onSurface.copy(alpha = 0.06f)).padding(Theme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                musicControlLayout(player, Modifier.width(Theme.size.cell1 * 1.5f))
                musicProgressLayout(player, musicInfo, Modifier.weight(1f))
            }
        }
        else {
            Column(
                modifier = modifier.padding(Theme.padding.value),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                musicProgressLayout(player, musicInfo, Modifier.fillMaxWidth())
                musicControlLayout(player, Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    override fun BasicContent() {
        Theme.ThemeModeWrapper(true) {
            Box(modifier = Modifier.fillMaxSize().background(Theme.color.background)) {
                val player = requirePlayer() ?: return@Box

                val deviceType by rememberDeviceType()
                val landscape = deviceType == Device.Type.LANDSCAPE
                val musicInfo = player.currentMusic

                LaunchedEffect(player.error) {
                    player.error?.then { slot.tip.error(it.message) }
                }

                // 壁纸
                Box(modifier = Modifier.fillMaxSize().blurSource(blurState).zIndex(1f)) {
                    if (musicInfo != null) {
                        val infoType = if (player.currentUseAnimationBackground) ModResourceType.Animation else ModResourceType.Background
                        val path = musicInfo.path(app.modPath, infoType)

                        LocalFileImage(
                            source = path,
                            version = path.path,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurTarget(blurState, MusicBlurParameters)
                        .padding(LocalImmersivePadding.current + Theme.padding.eValue9)
                        .zIndex(2f),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                ) {
                    ToolLayout(
                        player = player,
                        musicInfo = musicInfo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fastClipCircle()
                            .background(Theme.color.onSurface.copy(alpha = 0.06f))
                            .border(Theme.border.v10, Theme.color.onSurface.copy(alpha = 0.12f), Theme.shape.circle)
                            .condition(musicInfo != null) {
                                clickable { navigate(::ScreenMusicDetails, musicInfo.id) }
                            }.padding(Theme.padding.value)
                    )

                    if (landscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7)
                        ) {
                            featureLayout(player, Modifier.weight(1f).fillMaxHeight())
                            lyricsLayout(player, Modifier.width(Theme.size.cell1 * 1.5f).fillMaxHeight())
                        }
                    }
                    else {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                        ) {
                            featureLayout(player, Modifier.fillMaxWidth())
                            lyricsLayout(player, Modifier.fillMaxWidth().weight(1f))
                        }
                    }

                    PlaybackLayout(player, musicInfo, horizontal = landscape, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    private val currentPlaylistSheet = this land object : Sheet() {
        override val scrollable: Boolean = false
        override val maxPortraitRatio: Float = 0.85f

        @Composable
        override fun Content() {
            val player = requirePlayer() ?: return
            val playlist = DataSourceMusic.playlist
            val musicList = player.musicList
            val library = DataSourceMusic.library

            if (musicList.isNotEmpty()) {
                val currentIndex by rememberDerivedState { musicList.indexOf(player.currentId) }
                val isEmptyList by rememberDerivedState { musicList.isEmpty() }

                LaunchedEffect(isEmptyList) {
                    if (isEmptyList) close()
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleEllipsisText(text = "${playlist.name}(${musicList.size})", style = Theme.typography.v6.bold, color = Theme.color.secondary)
                        Icon(icon = Icons.StopCircle, onClick = {
                            close()
                            launch { player.stop() }
                        })
                    }
                    Divider()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = rememberLazyListState(if (currentIndex != -1) currentIndex else 0)
                    ) {
                        itemsIndexed(
                            items = musicList,
                            key = { _, id -> id }
                        ) { index, id ->
                            val isCurrent = index == currentIndex
                            val musicInfo = library[id]

                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable {
                                    launch { player.gotoIndex(index) }
                                    close()
                                }.padding(Theme.padding.value),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                            ) {
                                LocalFileImage(
                                    source = musicInfo?.path(app.modPath, ModResourceType.Record),
                                    modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(Theme.shape.v8)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                                ) {
                                    SimpleEllipsisText(
                                        text = musicInfo?.name ?: "未知歌曲",
                                        color = if (isCurrent) Theme.color.primary else LocalColor.current,
                                        style = if (isCurrent) Theme.typography.v7.bold else Theme.typography.v7
                                    )
                                    SimpleEllipsisText(
                                        text = musicInfo?.singer ?: "未知歌手",
                                        style = if (isCurrent) Theme.typography.v8.bold else Theme.typography.v8,
                                        color = if (isCurrent) Theme.color.primary else LocalColorVariant.current
                                    )
                                }
                                Box(
                                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) WaveLoading.Content(color = Theme.color.primary, modifier = Modifier.fillMaxSize(fraction = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
