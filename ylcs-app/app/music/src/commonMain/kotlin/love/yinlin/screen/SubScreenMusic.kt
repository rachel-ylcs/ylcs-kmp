package love.yinlin.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import love.yinlin.app
import love.yinlin.app.music.resources.Res
import love.yinlin.app.music.resources.img_music_record
import love.yinlin.common.PathMod
import love.yinlin.compose.*
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberRefState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.rememberOffScreenState
import love.yinlin.compose.screen.NavigationScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderIntConverter
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.node.blurSource
import love.yinlin.compose.ui.node.blurTarget
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.catching
import love.yinlin.extension.isFile
import love.yinlin.extension.lazyProvider
import love.yinlin.extension.timeString
import love.yinlin.media.lyrics.LyricsEngine
import love.yinlin.startup.StartupMusicPlayer
import kotlin.math.abs

@Stable
class SubScreenMusic(parent: NavigationScreen) : SubScreen(parent) {
    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    private var currentDebounceTime by mutableLongStateOf(0L)

    private var isAnimationBackground by mutableStateOf(false)
    private var hasAnimation by mutableStateOf(false)
    private var hasVideo by mutableStateOf(false)

    private val blurState = BlurState()

    private var sleepJob: Job? by mutableRefStateOf(null)

    override suspend fun initialize() {
        monitor(state = { mp?.position }) { position ->
            mp?.let { player ->
                if (position != null) {
                    // 处理进度条
                    if (abs(position - currentDebounceTime) > 1000L - player.engine.interval) currentDebounceTime = position
                    // 处理歌词
                    player.engine.update(position)
                    if (player.floatingLyrics.isAttached) player.floatingLyrics.update()
                }
            }
        }

        monitor(state = { mp?.currentMusic }) { music ->
            mp?.apply {
                // 重置引擎
                engine.reset()

                if (music != null) {
                    catching {
                        Coroutines.io {
                            // 按引擎顺序依次检查是否成功加载
                            val rootPath = music.path(PathMod)
                            for (engineType in app.config.lyricsEngineOrder) {
                                val newEngine = LyricsEngine[engineType]
                                if (newEngine.load(rootPath)) {
                                    engine = newEngine
                                    break
                                }
                            }

                            // 更新状态标志
                            hasAnimation = music.path(PathMod, ModResourceType.Animation).isFile
                            hasVideo = music.path(PathMod, ModResourceType.Video).isFile
                        }
                    }
                }
                else {
                    hasAnimation = false
                    hasVideo = false
                    sleepJob?.cancel()
                    sleepJob = null
                }

                if (isAnimationBackground && !hasAnimation) isAnimationBackground = false
            }
        }

        monitor(state = { mp?.error }) { error ->
            error?.let { slot.tip.error(it.message) }
        }
    }

    @Composable
    private fun MusicBackground(modifier: Modifier = Modifier) {
        val music = mp?.currentMusic
        if (music != null) {
            LocalFileImage(
                uri = music.path(PathMod, if (isAnimationBackground) ModResourceType.Animation else ModResourceType.Background).toString(),
                music, isAnimationBackground,
                contentScale = ContentScale.Crop,
                alpha = 0.85f,
                modifier = modifier
            )
        }
        else Box(modifier = modifier)
    }

    @Composable
    private fun ToolLayout(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            ActionScope.SplitContainer(
                modifier = Modifier.fillMaxWidth(),
                left = {
                    Icon(icon = Icons.LibraryMusic, tip = "曲库", onClick = {
                        if (mp?.isInit == true) navigate(::ScreenMusicLibrary)
                        else slot.tip.warning("播放器初始化失败")
                    })
                    Icon(icon = Icons.QueueMusic, tip = "歌单", onClick = {
                        if (mp?.isInit == true) navigate(::ScreenPlaylistLibrary)
                        else slot.tip.warning("播放器初始化失败")
                    })
                    Icon(icon = Icons.Lyrics, tip = "歌词", onClick = {
                        if (mp?.isInit == true) navigate(::ScreenLyricsSettings)
                        else slot.tip.warning("播放器初始化失败")
                    })
                },
                right = {
                    Icon(icon = Icons.AlarmOn, tip = "睡眠模式", onClick = {
                        if (mp != null) sleepModeSheet.open()
                        else slot.tip.warning("播放器初始化失败")
                    })
                }
            )

            val music = mp?.currentMusic

            AnimationContent(music?.name) {
                SimpleEllipsisText(text = it ?: "无音源", color = Colors.Green4, style = Theme.typography.v4.bold)
            }

            ActionScope.SplitContainer(
                modifier = Modifier.fillMaxWidth(),
                left = {
                    AnimationContent(music?.singer) {
                        SimpleEllipsisText(text = it ?: "未知歌手", color = Colors.Green1, style = Theme.typography.v6)
                    }
                },
                right = {
                    Icon(
                        icon = Icons.GifBox,
                        tip = "动画",
                        color = if (isAnimationBackground) Theme.color.primary else LocalColor.current,
                        enabled = hasAnimation,
                        onClick = { isAnimationBackground = !isAnimationBackground }
                    )
                    Icon(
                        icon = Icons.MusicVideo,
                        tip = "视频",
                        enabled = hasVideo,
                        onClick = {
                            mp?.let {
                                launch {
                                    it.pause()
                                    it.currentMusic?.path(PathMod, ModResourceType.Video)?.let { path ->
                                        navigate(::ScreenVideo, path.toString())
                                    }
                                }
                            }
                        }
                    )
                    Icon(icon = Icons.Comment, tip = "歌评", onClick = {
                        mp?.currentMusic?.let { navigate(::ScreenMusicDetails, it.id) }
                    })
                }
            )
        }
    }

    @Composable
    private fun MusicCover(musicInfo: MusicInfo, modifier: Modifier = Modifier) {
        var animationRecord by rememberRefState { Animatable(0f) }
        var lastDegree by rememberValueState(0f)
        val isForeground = rememberOffScreenState()

        val isPlaying = mp?.isPlaying ?: false

        LaunchedEffect(isPlaying, isForeground) {
            if (isPlaying && isForeground) {
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
            uri = musicInfo.path(PathMod, ModResourceType.Record).toString(),
            musicInfo,
            contentScale = ContentScale.Crop,
            modifier = modifier.rotate(degrees = animationRecord.value),
            onClick = { navigate(::ScreenMusicDetails, musicInfo.id) }
        )
    }

    @Composable
    private fun MusicCoverLayout(modifier: Modifier = Modifier) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(res = Res.drawable.img_music_record, modifier = Modifier.fillMaxSize().zIndex(1f))
            AnimationContent(
                state = mp?.currentMusic,
                duration = Theme.animation.duration.v1,
                enter = { fadeIn(animationSpec = tween(it)) },
                exit = { fadeOut(animationSpec = tween(it, delayMillis = it / 2)) },
                modifier = Modifier.fillMaxSize(fraction = 0.641f).clip(Theme.shape.circle).zIndex(2f)
            ) {
                if (it != null) {
                    MusicCover(musicInfo = it, modifier = Modifier.fillMaxSize().border(
                        width = Theme.border.v10,
                        color = Theme.color.outline,
                        shape = Theme.shape.circle
                    ))
                }
            }
        }
    }

    @Composable
    private fun MusicProgressLayout(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val position = currentDebounceTime
            val duration = mp?.duration?: 0L
            val progress = if (duration == 0L) 0f else position / duration.toFloat()
            val trackHeight = Theme.size.box4

            SimpleClipText(text = position.timeString)
            Slider(
                value = progress,
                onValueChangeFinished = { newProgress ->
                    launch { mp?.seekTo((newProgress * duration).toLong()) }
                },
                enabled = duration != 0L,
                trackHeight = trackHeight,
                trackColor = Colors.Gray3,
                activeColor = Colors.Green5,
                trackShape = Theme.shape.circle,
                showThumb = false,
                modifier = Modifier.weight(1f)
            ) {
                val chorus by rememberDerivedState { mp?.currentMusic?.chorus }
                if (chorus != null && duration != 0L) {
                    Box(modifier = Modifier.matchParentSize()) {
                        for (hotpot in chorus) {
                            val color by animateColorAsState(
                                targetValue = if (position >= hotpot) Colors.Green2 else LocalColor.current,
                                animationSpec = tween(durationMillis = 1000)
                            )

                            Box(modifier = Modifier
                                .align(BiasAlignment(horizontalBias = hotpot / duration.toFloat() * 2 - 1, verticalBias = 0f))
                                .width((trackHeight + Theme.padding.g) * 2)
                                .height(trackHeight * 2)
                                .silentClick {
                                    launch { mp?.seekTo(hotpot) }
                                }
                                .padding(horizontal = Theme.padding.g)
                                .background(color = color, shape = Theme.shape.circle)
                            )
                        }
                    }
                }
            }
            SimpleClipText(text = duration.timeString)
        }
    }

    @Composable
    private fun MusicControlLayout(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadingIcon(
                icon = when (mp?.playMode) {
                    MediaPlayMode.Loop -> Icons.LoopMode
                    MediaPlayMode.Random -> Icons.ShuffleMode
                    else -> Icons.OrderMode
                },
                onClick = { mp?.switchPlayMode() }
            )
            LoadingIcon(icon = Icons.GotoPrevious, color = Colors.Green1, onClick = { mp?.gotoPrevious() })
            Box(
                modifier = Modifier.wrapContentSize().clip(Theme.shape.circle)
                    .background(Colors.Green5).clickable {
                        launch {
                            mp?.let {
                                if (it.isPlaying) it.pause()
                                else it.play()
                            }
                        }
                    }.padding(Theme.padding.e),
                contentAlignment = Alignment.Center
            ) {
                val isPlaying = mp?.isPlaying ?: false
                Icon(
                    icon = if (isPlaying) Icons.Pause else Icons.Play,
                    modifier = Modifier.offset { if (isPlaying) IntOffset.Zero else IntOffset(1.5.dp.roundToPx(), 0) }
                )
            }
            LoadingIcon(icon = Icons.GotoNext, color = Colors.Green1, onClick = { mp?.gotoNext() })
            Icon(icon = Icons.Playlist, onClick = {
                if (mp?.isReady == true) currentPlaylistSheet.open()
            })
        }
    }

    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.fillMaxSize().background(ColorSystem.Default.dark.background),
            contentAlignment = Alignment.BottomCenter
        ) {
            ThemeContainer(ColorSystem.Default.dark.onBackground, ColorSystem.Default.dark.onBackgroundVariant) {
                val device = LocalDevice.current.type
                val immersivePadding = LocalImmersivePadding.current

                MusicBackground(modifier = Modifier.fillMaxSize().blurSource(blurState).zIndex(1f))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    ToolLayout(modifier = Modifier
                        .padding(immersivePadding.withoutBottom + Theme.padding.value9)
                        .fillMaxWidth()
                        .clip(Theme.shape.v5)
                        .blurTarget(blurState)
                        .padding(Theme.padding.value)
                    )

                    val player = mp
                    val isReady = player != null && player.isReady

                    if (device == Device.Type.PORTRAIT) {
                        if (isReady) {
                            MusicCoverLayout(modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(max = Theme.size.image2)
                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                .shadow(Theme.shape.circle, Theme.shadow.v3)
                            )
                        }
                    }
                    else {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(Theme.padding.eValue5),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isReady) {
                                MusicCoverLayout(modifier = Modifier
                                    .weight(1f, fill = false)
                                    .widthIn(max = Theme.size.image1)
                                    .aspectRatio(1f)
                                    .shadow(Theme.shape.circle, Theme.shadow.v3)
                                )

                                val width = if (device == Device.Type.LANDSCAPE) Theme.size.cell1 else Theme.size.cell2
                                Box(
                                    modifier = Modifier.width(width).fillMaxHeight().clip(Theme.shape.v5).blurTarget(blurState),
                                    contentAlignment = Alignment.Center
                                ) {
                                    player.engine.LyricsCanvas(config = app.config.lyricsEngineConfig, host = player.engineHost)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier
                        .padding(immersivePadding.withoutTop + PaddingValues(top = Theme.padding.v9))
                        .fillMaxWidth()
                        .blurTarget(blurState)
                    ) {
                        if (device == Device.Type.PORTRAIT && isReady) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(Theme.size.cell4),
                                contentAlignment = Alignment.Center
                            ) {
                                player.engine.LyricsCanvas(config = app.config.lyricsEngineConfig, host = player.engineHost)
                            }
                        }

                        if (device == Device.Type.LANDSCAPE) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MusicControlLayout(modifier = Modifier.weight(1f))
                                MusicProgressLayout(modifier = Modifier.weight(3f))
                            }
                        }
                        else {
                            MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(PaddingValues(start = Theme.padding.h9, end = Theme.padding.h9, top = Theme.padding.v9)))
                            MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9))
                        }
                    }
                }
            }
        }
    }

    private val currentPlaylistSheet = this land object : Sheet() {
        override val scrollable: Boolean = false

        @Composable
        override fun Content() {
            val items = mp?.playlist
            val musicList = mp?.musicList
            val library = mp?.library

            if (items != null && musicList != null && library != null) {
                val currentIndex by rememberDerivedState { musicList.indexOf(mp?.currentId) }
                val isEmptyList by rememberDerivedState { musicList.isEmpty() }

                LaunchedEffect(isEmptyList) {
                    if (isEmptyList) close()
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleEllipsisText(text = items.name, style = Theme.typography.v6.bold, color = Theme.color.secondary)
                        Icon(icon = Icons.StopCircle, onClick = {
                            close()
                            launch { mp?.stop() }
                        })
                    }
                    Divider()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = rememberLazyListState(if (currentIndex != -1) currentIndex else 0)
                    ) {
                        itemsIndexed(
                            items = musicList,
                            key = { _, info -> info }
                        ) { index, id ->
                            val isCurrent = index == currentIndex
                            val musicInfo = library[id]

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    launch { mp?.gotoIndex(index) }
                                    close()
                                }.padding(Theme.padding.value),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SimpleEllipsisText(
                                    text = musicInfo?.name ?: "未知歌曲",
                                    color = if (isCurrent) Theme.color.primary else LocalColor.current,
                                    style = if (isCurrent) Theme.typography.v7.bold else Theme.typography.v7,
                                    modifier = Modifier.weight(2f),
                                    textAlign = TextAlign.Start
                                )
                                SimpleEllipsisText(
                                    text = musicInfo?.singer ?: "未知歌手",
                                    style = if (isCurrent) Theme.typography.v8.bold else Theme.typography.v8,
                                    color = if (isCurrent) Theme.color.primary else LocalColorVariant.current,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val sleepModeSheet = this land object : Sheet() {
        private var sleepRemainSeconds: Int by mutableIntStateOf(0)

        private fun startSleepMode(seconds: Int) {
            sleepJob?.cancel()
            sleepJob = launch {
                sleepRemainSeconds = seconds
                repeat(seconds) {
                    delay(1000L)
                    --sleepRemainSeconds
                }
                mp?.stop()
                sleepJob = null
            }
        }

        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
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
                        text = if (sleepJob == null) "启动" else "停止",
                        icon = if (sleepJob == null) Icons.AlarmOn else Icons.AlarmOff,
                        onClick = {
                            if (sleepJob == null) {
                                val sleepTime = sleepHour * 3600 + sleepMinutes * 60
                                if (sleepTime > 0) startSleepMode(sleepTime)
                                else slot.tip.warning("未设定时间")
                            }
                            else {
                                sleepJob?.cancel()
                                sleepJob = null
                            }
                        }
                    )
                }
                if (sleepJob == null) {
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
                else {
                    SimpleEllipsisText(text = "关闭播放器倒计时")
                    SimpleEllipsisText(
                        text = (sleepRemainSeconds * 1000L).timeString,
                        style = Theme.typography.v4.bold,
                        color = Theme.color.secondary
                    )
                }
            }
        }
    }
}