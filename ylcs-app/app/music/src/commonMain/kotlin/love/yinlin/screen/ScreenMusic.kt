package love.yinlin.screen

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collectLatest
import love.yinlin.app
import love.yinlin.app.music.resources.Res
import love.yinlin.app.music.resources.img_music_record
import love.yinlin.common.SleepTimer
import love.yinlin.compose.*
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.compose.extension.*
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.animation.WaveLoading
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.floating.DialogModFactory
import love.yinlin.compose.ui.floating.Menus
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.LoadingTextButton
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderIntConverter
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.node.*
import love.yinlin.compose.ui.text.FastFixedText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.window.rememberFocusWindowState
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.then
import love.yinlin.extension.timeString
import love.yinlin.startup.StartupMusicPlayer
import kotlin.math.abs

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
    private fun ToolLayout(player: StartupMusicPlayer, musicInfo: MusicInfo?, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            AnimationContent(
                state = musicInfo?.name,
                modifier = Modifier.fillMaxWidth().padding(start = Theme.padding.h10, end = Theme.padding.h10, top = Theme.padding.v)
            ) {
                SimpleEllipsisText(text = it ?: "无音源", color = Colors.Green4, style = Theme.typography.v4.bold)
            }

            ActionScope.SplitContainer(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h10),
                left = {
                    AnimationContent(musicInfo?.singer) {
                        SimpleEllipsisText(text = it ?: "未知歌手", color = Colors.Green1, style = Theme.typography.v6)
                    }
                },
                right = {
                    Icon(
                        icon = Icons.GifBox,
                        tip = "动画",
                        color = when {
                            player.currentUseAnimationBackground -> Theme.color.primary
                            player.currentHasAnimation -> Theme.color.secondary
                            else -> LocalColor.current
                        },
                        onClick = {
                            if (player.currentHasAnimation) player.currentUseAnimationBackground = !player.currentUseAnimationBackground
                            else slot.tip.warning("未安装动画资源")
                        }
                    )
                    Icon(
                        icon = Icons.MusicNote,
                        tip = "伴奏",
                        color = if (player.currentHasAccompaniment) Theme.color.secondary else LocalColor.current,
                        onClick = {
                            if (musicInfo != null) {
                                if (player.currentHasAccompaniment) {
                                    launch {
                                        player.pause()
                                        navigate(::ScreenAccompaniment, musicInfo, player.engine.type)
                                    }
                                }
                                else slot.tip.warning("未安装伴奏资源")
                            }
                        }
                    )
                    Icon(
                        icon = Icons.MusicVideo,
                        tip = "视频",
                        color = if (player.currentHasVideo) Theme.color.secondary else LocalColor.current,
                        onClick = {
                            if (musicInfo != null) {
                                if (player.currentHasVideo) {
                                    launch {
                                        player.pause()
                                        navigate(::ScreenVideo, musicInfo.path(app.modPath, ModResourceType.Video).path)
                                    }
                                }
                                else slot.tip.warning("未安装视频资源")
                            }
                        }
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showMenu by rememberFalse()

                TextButton(icon = Icons.LibraryMusic, text = "曲库", onClick = {
                    navigate(::ScreenMusicLibrary)
                })
                TextButton(icon = Icons.QueueMusic, text = "歌单", onClick = {
                    navigate(::ScreenPlaylistLibrary)
                })
                LoadingTextButton(icon = Icons.Token, text = "工坊", onClick = {
                    when (val result = modFactoryDialog.open()) {
                        null -> {}
                        is DialogModFactory.ModResult.FromCenter -> navigate(::ScreenModCenter)
                        is DialogModFactory.ModResult.FromCreate -> navigate(::ScreenCreateMusic)
                        is DialogModFactory.ModResult.FromImport -> navigate(::ScreenImportMusic, null)
                        is DialogModFactory.ModResult.FromPlatform -> navigate(::ScreenPlatformMusic, null, result.type)
                    }
                })

                Box(modifier = Modifier.weight(1f))

                Menus(
                    visible = showMenu,
                    onClose = { showMenu = false },
                    menus = {
                        Menu(text = "歌词设置", icon = Icons.Lyrics, onClick = {
                            navigate(::ScreenLyricsSettings)
                        })
                        Menu(text = "睡眠模式", icon = Icons.AlarmOn, onClick = {
                            if (!player.isReady) slot.tip.warning("未启动播放器")
                            else sleepModeSheet.open(player.sleepTimer)
                        })
                    }
                ) {
                    TextButton(text = "更多", icon = Icons.Add, onClick = { showMenu = true })
                }
            }
        }
    }

    @Composable
    private fun MusicCover(player: StartupMusicPlayer, musicInfo: MusicInfo, modifier: Modifier = Modifier) {
        var animationRecord by rememberRefState { Animatable(0f) }
        var lastDegree by rememberValueState(0f)
        val isFocus by rememberFocusWindowState()

        LaunchedEffect(player.isPlaying, isFocus) {
            if (player.isPlaying && isFocus) {
                animationRecord.animateTo(
                    targetValue = 360f + lastDegree,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 15000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                ) {
                    lastDegree = this.value
                }
            }
            else {
                animationRecord.snapTo(lastDegree)
                animationRecord.stop()
            }
        }

        LocalFileImage(
            uri = musicInfo.path(app.modPath, ModResourceType.Record).path,
            musicInfo,
            contentScale = ContentScale.Crop,
            modifier = modifier.fastRotate(animationRecord),
            onClick = {
                navigate(::ScreenMusicDetails, musicInfo.id)
            }
        )
    }

    @Composable
    private fun MusicCoverLayout(player: StartupMusicPlayer, musicInfo: MusicInfo?, modifier: Modifier) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(res = Res.drawable.img_music_record, modifier = Modifier.fillMaxSize().zIndex(1f))
            AnimationContent(
                state = musicInfo,
                duration = Theme.animation.duration.v1,
                enter = { fadeIn(animationSpec = tween(it)) },
                exit = { fadeOut(animationSpec = tween(it, delayMillis = it / 2)) },
                modifier = Modifier.fillMaxSize(fraction = 0.641f).fastClipCircle().zIndex(2f)
            ) { info ->
                if (info != null) {
                    MusicCover(player, info, modifier = Modifier.fillMaxSize().border(
                        width = Theme.border.v10,
                        color = Theme.color.outline,
                        shape = Theme.shape.circle
                    ))
                }
            }
        }
    }

    @Composable
    private fun LyricsLayout(player: StartupMusicPlayer, modifier: Modifier) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            player.engine.LyricsCanvas(config = app.config.lyricsEngineConfig, host = player.engineHost)
        }
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

                isDragging = false
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

    @Composable
    private fun MusicProgressLayout(player: StartupMusicPlayer, musicInfo: MusicInfo?, modifier: Modifier) {
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
    private fun MusicControlLayout(player: StartupMusicPlayer, modifier: Modifier) {
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
                onClick = { player.switchPlayMode() }
            )
            LoadingIcon(icon = Icons.GotoPrevious, color = Colors.Green1, onClick = { player.gotoPrevious() })
            Box(
                modifier = Modifier.wrapContentSize()
                    .fastClipCircle()
                    .background(Colors.Green5).clickable {
                        launch {
                            if (player.isPlaying) player.pause()
                            else player.play()
                        }
                    }.padding(Theme.padding.e),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon = if (player.isPlaying) Icons.Pause else Icons.Play,
                    modifier = Modifier.fastOffsetX { if (player.isPlaying) null else 1.5.dp.toPx() }
                )
            }
            LoadingIcon(icon = Icons.GotoNext, color = Colors.Green1, onClick = { player.gotoNext() })
            Icon(icon = Icons.Playlist, onClick = {
                if (player.isReady) currentPlaylistSheet.open()
            })
        }
    }

    @Composable
    override fun BasicContent() {
        val player = requirePlayer() ?: return

        Theme.ThemeModeWrapper(true) {
            Box(modifier = Modifier.fillMaxSize().background(Theme.color.background)) {
                val musicInfo = player.currentMusic

                LaunchedEffect(player.error) {
                    player.error?.then { slot.tip.error(it.message) }
                }

                Box(modifier = Modifier.fillMaxSize().blurSource(blurState).zIndex(1f)) {
                    if (musicInfo != null) {
                        val infoType = if (player.currentUseAnimationBackground) ModResourceType.Animation else ModResourceType.Background
                        val path = musicInfo.path(app.modPath, infoType).path
                        LocalFileImage(uri = path, path, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().blurTarget(blurState).padding(LocalImmersivePadding.current).zIndex(2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val deviceType by rememberDeviceType()

                    ToolLayout(player, musicInfo, modifier = Modifier.fillMaxWidth())

                    if (player.isReady) {
                        if (deviceType == Device.Type.PORTRAIT) {
                            MusicCoverLayout(player, musicInfo, modifier = Modifier
                                .padding(Theme.padding.value7)
                                .heightIn(max = Theme.size.image2)
                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                .shadow(Theme.shape.circle, Theme.shadow.v3)
                            )
                            LyricsLayout(player, modifier = Modifier.fillMaxWidth().weight(1f))
                        }
                        else {
                            Row(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isLandscape = deviceType == Device.Type.LANDSCAPE
                                val padding = if (isLandscape) Theme.padding.eValue5 else Theme.padding.eValue7

                                MusicCoverLayout(player, musicInfo, modifier = Modifier
                                    .padding(padding)
                                    .widthIn(max = if (isLandscape) Theme.size.image1 else Theme.size.image2)
                                    .aspectRatio(1f)
                                    .shadow(Theme.shape.circle, Theme.shadow.v3)
                                )

                                LyricsLayout(player, modifier = Modifier
                                    .padding(padding)
                                    .widthIn(max = if (isLandscape) Theme.size.cell1 * 1.5f else Theme.size.cell1)
                                    .fillMaxHeight()
                                )
                            }
                        }
                    }
                    else Box(modifier = Modifier.weight(1f))

                    if (deviceType == Device.Type.LANDSCAPE) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MusicControlLayout(player, modifier = Modifier.weight(1f))
                            MusicProgressLayout(player, musicInfo, modifier = Modifier.weight(3f))
                        }
                    }
                    else {
                        MusicProgressLayout(player, musicInfo, modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9))
                        MusicControlLayout(player, modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9))
                    }
                }
            }
        }
    }

    private val modFactoryDialog = this land DialogModFactory()

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
                                    uri = musicInfo?.path(app.modPath, ModResourceType.Record)?.path ?: "",
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
                                    if (isCurrent) WaveLoading.Content(Theme.color.primary, modifier = Modifier.fillMaxSize(fraction = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val sleepModeSheet = this land object : SheetContent<SleepTimer>() {
        @Composable
        override fun Content(args: SleepTimer) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                var sleepHour by rememberValueState(0)
                var sleepMinutes by rememberValueState(0)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = "睡眠模式", style = Theme.typography.v6.bold, color = Theme.color.primary)
                    PrimaryTextButton(
                        text = if (args.isRunning) "停止" else "启动",
                        icon = if (args.isRunning) Icons.AlarmOff else Icons.AlarmOn,
                        onClick = {
                            if (args.isRunning) args.stop()
                            else {
                                val sleepTime = sleepHour * 3600 + sleepMinutes * 60
                                if (sleepTime > 0) args.start(sleepTime)
                                else slot.tip.warning("未设定时间")
                            }
                        }
                    )
                }
                if (args.isRunning) {
                    SimpleEllipsisText(text = "关闭播放器倒计时")
                    SimpleEllipsisText(
                        text = (args.remainSeconds * 1000L).timeString,
                        style = Theme.typography.v4.bold,
                        color = Theme.color.secondary
                    )
                }
                else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleEllipsisText(
                            text = "${sleepHour}小时",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = sleepHour,
                            converter = remember { SliderIntConverter(0, 11) },
                            onValueChangeFinished = { sleepHour = it },
                            modifier = Modifier.weight(3f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleEllipsisText(
                            text = "${sleepMinutes}分钟",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = sleepMinutes,
                            converter = remember { SliderIntConverter(0, 59) },
                            onValueChangeFinished = { sleepMinutes = it },
                            modifier = Modifier.weight(3f)
                        )
                    }
                }
            }
        }
    }
}