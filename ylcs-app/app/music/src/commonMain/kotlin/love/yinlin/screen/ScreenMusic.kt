package love.yinlin.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.node.*
import love.yinlin.compose.ui.text.FastFixedText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
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

    private val sleepHour = StepperState()
    private val sleepMinutes = StepperState()

    @Composable
    private fun ResourceControl(text: String, icon: ImageVector, color: Color, horizontal: Boolean, onClick: () -> Unit) {
        val modifier = Modifier.width(if (horizontal) Theme.size.image7 else Theme.size.input8).clip(Theme.shape.v7).clickable(onClick = onClick).padding(vertical = Theme.padding.v)

        if (horizontal) {
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon = icon, color = color)
                SimpleEllipsisText(text = text, style = Theme.typography.v8, color = color)
            }
        }
        else {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Theme.padding.g)) {
                Icon(icon = icon, color = color)
                SimpleEllipsisText(text = text, style = Theme.typography.v9, color = color)
            }
        }
    }

    @Composable
    private fun ToolLayout(player: StartupMusicPlayer, musicInfo: MusicInfo?, deviceType: Device.Type, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier.clip(Theme.shape.circle)
                .background(Theme.color.onSurface.copy(alpha = 0.06f))
                .border(Theme.border.v10, Theme.color.onSurface.copy(alpha = 0.12f), Theme.shape.circle)
                .clickable(enabled = musicInfo != null) { musicInfo?.then { navigate(::ScreenMusicDetails, it.id) } }
                .padding(Theme.padding.value),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicCoverLayout(musicInfo, modifier = Modifier.size(Theme.size.image7))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                AnimationContent(state = musicInfo?.name, modifier = Modifier.fillMaxWidth()) {
                    SimpleEllipsisText(text = it ?: "无音源", color = Theme.color.onSurface, style = if (deviceType == Device.Type.LANDSCAPE) Theme.typography.v5.bold else Theme.typography.v6.bold)
                }
                AnimationContent(state = musicInfo?.singer, modifier = Modifier.fillMaxWidth()) {
                    SimpleEllipsisText(text = it ?: "未知歌手", color = Theme.color.onSurfaceVariant, style = Theme.typography.v8)
                }
            }
            Box(modifier = Modifier.width(Theme.border.v10).height(Theme.size.image8).background(Theme.color.outline.copy(alpha = 0.35f)))
            ResourceControl(
                text = "动画",
                icon = Icons.GifBox,
                horizontal = deviceType != Device.Type.PORTRAIT,
                color = when {
                    player.currentUseAnimationBackground -> Theme.color.secondary
                    player.currentHasAnimation -> Theme.color.primary
                    else -> Theme.color.onSurfaceVariant
                },
                onClick = {
                    if (player.currentHasAnimation) player.currentUseAnimationBackground = !player.currentUseAnimationBackground
                    else slot.tip.warning("未安装动画资源")
                }
            )
            ResourceControl(
                text = "伴奏",
                icon = Icons.MusicNote,
                horizontal = deviceType != Device.Type.PORTRAIT,
                color = if (player.currentHasAccompaniment) Theme.color.secondary else Theme.color.onSurfaceVariant,
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
            ResourceControl(
                text = "视频",
                icon = Icons.MusicVideo,
                horizontal = deviceType != Device.Type.PORTRAIT,
                color = if (player.currentHasVideo) Theme.color.secondary else Theme.color.onSurfaceVariant,
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
    }

    @Composable
    private fun MusicCover(musicInfo: MusicInfo, modifier: Modifier = Modifier) {
        LocalFileImage(
            source = musicInfo.path(app.modPath, ModResourceType.Record),
            version = "${musicInfo.id}-${musicInfo.modification}",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }

    @Composable
    private fun MusicCoverLayout(musicInfo: MusicInfo?, modifier: Modifier) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(res = Res.drawable.img_music_record, modifier = Modifier.fillMaxSize().zIndex(1f))
            AnimationContent(
                state = musicInfo,
                duration = Theme.animation.duration.v1,
                enter = { fadeIn(animationSpec = tween(it)) },
                exit = { fadeOut(animationSpec = tween(it, delayMillis = it / 2)) },
                modifier = Modifier.fillMaxSize(fraction = 0.641f).fastClipCircle().zIndex(2f)
            ) { info ->
                if (info != null) MusicCover(info, modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun NavigationTile(text: String, modifier: Modifier = Modifier, icon: ImageVector, onClick: () -> Unit) {
        Row(
            modifier = modifier.clip(Theme.shape.v5)
                .background(Theme.color.onSurface.copy(alpha = 0.06f))
                .border(Theme.border.v10, Theme.color.onSurface.copy(alpha = 0.12f), Theme.shape.v5)
                .clickable(onClick = onClick).padding(horizontal = Theme.padding.v, vertical = Theme.padding.v),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.v),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = icon, color = Theme.color.primary)
            SimpleEllipsisText(text = text, color = Theme.color.onSurface, style = Theme.typography.v7.bold, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun WorkshopEntry(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Column(
            modifier = modifier.clip(Theme.shape.v7)
                .background(Theme.color.onSurface.copy(alpha = 0.06f))
                .clickable(onClick = onClick).padding(horizontal = Theme.padding.g2, vertical = Theme.padding.g),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.g, Alignment.CenterVertically)
        ) {
            Icon(icon = icon, color = Theme.color.primary, modifier = Modifier.size(Theme.size.smallIcon))
            Text(text = text, style = Theme.typography.v7.bold, color = Theme.color.onSurface, textAlign = TextAlign.Center, maxLines = 1)
        }
    }

    @Composable
    private fun NavigationLayout(deviceType: Device.Type, modifier: Modifier = Modifier) {
        val covers = DataSourceMusic.library.values.take(4)

        Row(
            modifier = modifier.height(Theme.size.image5),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9)
        ) {
            Box(
                modifier = Modifier.width(if (deviceType == Device.Type.LANDSCAPE) Theme.size.image3 else Theme.size.image4).fillMaxHeight().clip(Theme.shape.v5)
                    .background(Theme.color.onSurface.copy(alpha = 0.06f))
                    .border(Theme.border.v10, Theme.color.onSurface.copy(alpha = 0.12f), Theme.shape.v5)
                    .clickable { navigate(::ScreenMusicLibrary) }.padding(Theme.padding.value)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.v), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon = Icons.LibraryMusic, color = Theme.color.primary, modifier = Modifier.size(Theme.size.smallIcon))
                    SimpleEllipsisText(text = "曲库", style = Theme.typography.v7.bold, color = Theme.color.onSurface)
                }
                if (covers.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(if (deviceType == Device.Type.LANDSCAPE) Theme.size.image7 else Theme.size.image8).align(Alignment.BottomStart)) {
                        for (index in covers.indices.reversed()) {
                            val music = covers[index]
                            LocalFileImage(
                                source = music.path(app.modPath, ModResourceType.Record),
                                version = music.modification.toString(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.75f,
                                modifier = Modifier.offset(x = Theme.padding.h10 * index).fillMaxHeight().aspectRatio(1f)
                                    .clip(Theme.shape.v7).border(Theme.border.v10, Theme.color.onSurface.copy(alpha = 0.2f), Theme.shape.v7)
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(Theme.padding.h10), verticalAlignment = Alignment.CenterVertically) {
                    NavigationTile(text = "歌单", icon = Icons.QueueMusic, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { navigate(::ScreenPlaylistLibrary) })
                    NavigationTile(text = "歌词", icon = Icons.Lyrics, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { navigate(::ScreenLyricsSettings) })
                }

                var workshopExpanded by rememberFalse()

                AnimationContent(
                    state = workshopExpanded,
                    modifier = Modifier.fillMaxWidth().weight(1.5f).clip(Theme.shape.v7),
                    enter = { duration -> slideInHorizontally(tween(duration)) { it } + fadeIn(tween(duration)) },
                    exit = { duration -> slideOutHorizontally(tween(duration)) { -it } + fadeOut(tween(duration)) }
                ) { expanded ->
                    if (expanded) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Theme.padding.e)) {
                            WorkshopEntry(text = "银临", icon = Icons.Token, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { navigate(::ScreenModCenter) })
                            WorkshopEntry(text = "平台", icon = Icons.CloudDownload, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { navigate(::ScreenPlatformMusic, null, null) })
                            WorkshopEntry(text = "导入", icon = Icons.Upload, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { navigate(::ScreenImportMusic, null) })
                            WorkshopEntry(text = "创建", icon = Icons.DesignServices, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { navigate(::ScreenCreateMusic) })
                        }
                    }
                    else NavigationTile(text = "工坊", icon = Icons.Token, modifier = Modifier.fillMaxSize(), onClick = { workshopExpanded = true })
                }
            }
        }
    }

    @Composable
    private fun SleepLayout(player: StartupMusicPlayer, modifier: Modifier = Modifier) {
        val timer = player.sleepTimer

        Row(
            modifier = modifier.clip(Theme.shape.v5)
                .background(Theme.color.onSurface.copy(alpha = 0.06f))
                .border(Theme.border.v10, Theme.color.onSurface.copy(alpha = 0.12f), Theme.shape.v5)
                .padding(horizontal = Theme.padding.h, vertical = Theme.padding.v),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.v),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(text = "睡眠", style = Theme.typography.v7.bold, color = Theme.color.onSurface)
            Icon(
                icon = if (timer.isRunning) Icons.AlarmOff else Icons.AlarmOn,
                tip = if (timer.isRunning) "停止睡眠定时" else "启动睡眠定时",
                color = if (timer.isRunning) Theme.color.warning else Theme.color.primary,
                enabled = player.isReady,
                onClick = {
                    if (timer.isRunning) timer.stop()
                    else {
                        val sleepTime = sleepHour.value * 3600 + sleepMinutes.value * 60
                        if (sleepTime > 0) timer.start(sleepTime)
                        else slot.tip.warning("未设定时间")
                    }
                }
            )
            if (timer.isRunning) {
                SimpleEllipsisText(text = (timer.remainSeconds * 1000L).timeString, style = Theme.typography.v6.bold, color = Theme.color.secondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            else {
                Spacer(modifier = Modifier.weight(1f))
                Stepper(
                    state = sleepHour,
                    enabled = player.isReady,
                    faster = true,
                    min = 0,
                    max = 11,
                    label = "小时",
                    converter = { it.toString().padStart(2, '0') }
                )
                Stepper(
                    state = sleepMinutes,
                    enabled = player.isReady,
                    faster = true,
                    min = 0,
                    max = 59,
                    label = "分钟",
                    converter = { it.toString().padStart(2, '0') }
                )
            }
        }
    }

    @Composable
    private fun FeatureLayout(player: StartupMusicPlayer, deviceType: Device.Type, scrollState: ScrollState, modifier: Modifier = Modifier) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)) {
            NavigationLayout(deviceType = deviceType, modifier = Modifier.fillMaxWidth().weight(1f, false).verticalScroll(scrollState))
            SleepLayout(player, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun LyricsPanel(player: StartupMusicPlayer, modifier: Modifier = Modifier) {
        if (player.isReady) LyricsLayout(player, modifier = modifier)
        else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                SimpleEllipsisText(text = "从曲库选择一首歌", color = Theme.color.onSurfaceVariant)
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
                tip = "播放模式",
                onClick = { player.switchPlayMode() }
            )
            LoadingIcon(icon = Icons.GotoPrevious, color = Theme.color.primary, tip = "上一首", onClick = { player.gotoPrevious() })
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
            LoadingIcon(icon = Icons.GotoNext, color = Theme.color.primary, tip = "下一首", onClick = { player.gotoNext() })
            Icon(icon = Icons.Playlist, tip = "当前播放列表", onClick = {
                if (player.isReady) currentPlaylistSheet.open()
            })
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
                MusicControlLayout(player, modifier = Modifier.width(Theme.size.cell1 * 1.5f))
                MusicProgressLayout(player, musicInfo, modifier = Modifier.weight(1f))
            }
        }
        else {
            Column(modifier = modifier.padding(Theme.padding.value), verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                MusicProgressLayout(player, musicInfo, modifier = Modifier.fillMaxWidth())
                MusicControlLayout(player, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    override fun BasicContent() {
        val player = requirePlayer() ?: return
        val deviceType by rememberDeviceType()

        Theme.ThemeModeWrapper(true) {
            Box(modifier = Modifier.fillMaxSize().background(Theme.color.background)) {
                val musicInfo = player.currentMusic

                LaunchedEffect(player.error) {
                    player.error?.then { slot.tip.error(it.message) }
                }

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

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().blurTarget(blurState, MusicBlurParameters).padding(LocalImmersivePadding.current).zIndex(2f)
                ) {
                    val landscape = deviceType != Device.Type.PORTRAIT
                    val minLyricsHeight = with(LocalDensity.current) { Theme.typography.v6.lineHeight.toDp() * 7 + Theme.typography.v5.lineHeight.toDp() } + Theme.padding.v * 16
                    val horizontalPadding = if (deviceType == Device.Type.LANDSCAPE) Theme.padding.e9 else Theme.padding.h
                    val maxFeatureWidth = (maxWidth - horizontalPadding * 2 - Theme.padding.h7 - Theme.size.cell2).coerceAtLeast(Theme.size.cell3)
                    val featureWidth = (maxWidth * 0.5f).coerceIn(Theme.size.cell1 * 1.2f, Theme.size.cell1 * 1.8f).coerceAtMost(maxFeatureWidth)
                    val featureScrollState = rememberScrollState()
                    val spacing = if (deviceType == Device.Type.LANDSCAPE) Theme.padding.v5 else Theme.padding.v7

                    Column(
                        modifier = Modifier.fillMaxSize().padding(if (deviceType == Device.Type.LANDSCAPE) Theme.padding.eValue9 else Theme.padding.value),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        ToolLayout(player, musicInfo, deviceType = deviceType, modifier = Modifier.fillMaxWidth())

                        if (landscape) {
                            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7)) {
                                FeatureLayout(player, deviceType = deviceType, scrollState = featureScrollState, modifier = Modifier
                                    .width(featureWidth)
                                    .fillMaxHeight()
                                )
                                LyricsPanel(player, modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                        else {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                val featureMaxHeight = (maxHeight - minLyricsHeight - spacing).coerceAtLeast(Theme.size.input7)

                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                                    FeatureLayout(player, deviceType = deviceType, scrollState = featureScrollState, modifier = Modifier.fillMaxWidth().heightIn(max = featureMaxHeight))
                                    LyricsPanel(player, modifier = Modifier.fillMaxWidth().weight(1f))
                                }
                            }
                        }

                        PlaybackLayout(player, musicInfo, horizontal = deviceType == Device.Type.LANDSCAPE, modifier = Modifier.fillMaxWidth())
                    }
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
