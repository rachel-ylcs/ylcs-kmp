package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.app.game_rhyme.resources.Res
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.common.PathMod
import love.yinlin.common.downloadCacheWithPath
import love.yinlin.common.rhyme.*
import love.yinlin.common.rhyme.data.ActionResult
import love.yinlin.compose.*
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.common.ArgsSlider
import love.yinlin.compose.ui.common.ResumeNumberBrush
import love.yinlin.compose.ui.common.RhymeAcrylicButton
import love.yinlin.compose.ui.common.RhymeAcrylicSurface
import love.yinlin.compose.ui.common.RhymeButton
import love.yinlin.compose.ui.common.RhymeMusicCard
import love.yinlin.compose.ui.common.RhymeOverlayLayout
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.common.value
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderLongConverter
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.StrokeText
import love.yinlin.compose.ui.text.Text
import love.yinlin.concurrent.atomic
import love.yinlin.coroutines.ioContext
import love.yinlin.cs.NetClient
import love.yinlin.cs.ServerRes
import love.yinlin.cs.url
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.catching
import love.yinlin.extension.catchingError
import love.yinlin.extension.exists
import love.yinlin.extension.readByteArray
import love.yinlin.extension.readText
import love.yinlin.foundation.Orientation
import love.yinlin.foundation.OrientationController
import love.yinlin.startup.StartupMusicPlayer

@Stable
class ScreenRhyme : Screen() {
    private var state: GameState by mutableRefStateOf(GameState.Loading)
    private var lockState: GameLockState by mutableRefStateOf(GameLockState.Normal)

    private var library = emptyList<RhymeMusic>()
    private var showEnabled by mutableStateOf(false)

    private val orientationController = OrientationController(app.context)

    private val rhymeManager = RhymeManager(
        context = app.context,
        onComplete = ::completeGame,
        onPause = { pauseGame(GameLockState.Pause) }
    )

    private var resumePauseJob: Job? = null

    private val orientationStarter = LaunchFlag()

    private var prologueBackground: Path? = null

    private fun onScreenOrientationChanged(type: Device.Type) {
        if (type == Device.Type.LANDSCAPE) {
            // 如果处于竖屏锁, 当转回横屏后启动恢复协程
            if (lockState is GameLockState.PortraitLock) {
                if (state is GameState.Playing) resumePauseTimer()
                else lockState = GameLockState.Normal
            }
        }
        else pauseGame(GameLockState.PortraitLock)
    }

    private fun startGame(info: MusicInfo, playConfig: RhymePlayConfig) {
        launch {
            val task1 = async(ioContext) {
                catchingNull {
                    info.path(PathMod, ModResourceType.Rhyme).readText()!!.parseJsonValue<RhymeLyricsConfig>()
                }
            }
            val task2 = async(ioContext) {
                info.path(PathMod, ModResourceType.Record).readByteArray()
            }
            val lyricsConfig = task1.await()
            val recordImage = task2.await()
            catchingError {
                require(lyricsConfig != null) { "歌词资源文件丢失或损坏" }
                require(lyricsConfig.id == info.id) { "歌词资源文件与MOD不匹配" }
                require(recordImage != null) { "封面资源文件丢失" }
                rhymeManager.apply {
                    start(
                        playConfig = playConfig,
                        name = info.name,
                        lyricsConfig = lyricsConfig,
                        recordImage = recordImage,
                        audio = info.path(PathMod, ModResourceType.Audio)
                    )
                }
                state = GameState.Playing(playConfig, info)
            }.errorTip
        }
    }

    private fun stopGame() {
        rhymeManager.stop()
        state = GameState.Start
    }

    private fun completeGame(playResult: RhymePlayResult) {
        (state as? GameState.Playing)?.let {
            state = GameState.Settling(RhymeResult(
                playConfig = it.playConfig,
                musicInfo = it.musicInfo,
                playResult = playResult
            ))
        }
    }

    private fun pauseGame(newState: GameLockState) {
        resumePauseJob?.cancel()
        resumePauseJob = null
        if (state is GameState.Playing) rhymeManager.pause()
        lockState = newState
    }

    private fun resumePauseTimer() {
        if (resumePauseJob == null) {
            resumePauseJob = launch {
                catching {
                    // 倒计时解除暂停状态
                    lockState = GameLockState.Resume(RhymeConfig.PAUSE_TIME)
                    repeat(RhymeConfig.PAUSE_TIME) {
                        delay(1000L)
                        --(lockState as? GameLockState.Resume)?.time
                    }
                    lockState = GameLockState.Normal
                    rhymeManager.apply { resume() }
                }
                resumePauseJob = null
            }
        }
    }

    @Composable
    private fun GameMaskPortraitLock() {
        RhymeAcrylicSurface(
            shape = Theme.shape.v3,
            contentPadding = Theme.padding.eValue7
        ) {
            ThemeContainer(Colors.Ghost) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v5, Alignment.CenterVertically)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircleLoading.Content(modifier = Modifier.size(Theme.size.image7).zIndex(1f))
                        Icon(icon = Icons.Lock, modifier = Modifier.size(Theme.size.image8).zIndex(2f))
                    }
                    SimpleClipText(text = "请保持横屏", style = Theme.typography.v4.bold)
                }
            }
        }
    }

    @Composable
    private fun GameMaskPause() {
        RhymeAcrylicSurface(
            shape = Theme.shape.v3,
            contentPadding = Theme.padding.eValue7
        ) {
            ThemeContainer(Colors.Ghost) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v5, Alignment.CenterVertically)
                ) {
                    SimpleClipText(text = "暂停中", style = Theme.typography.v4.bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h5),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RhymeAcrylicButton(icon = Icons.Clear, onClick = {
                            lockState = GameLockState.Normal
                            stopGame()
                        })
                        RhymeAcrylicButton(icon = Icons.PlayArrow, onClick = ::resumePauseTimer)
                    }
                }
            }
        }
    }

    @Composable
    private fun GameMaskResume(resumeState: GameLockState.Resume) {
        val style = Theme.typography.v1

        StrokeText(
            text = resumeState.timeString,
            color = Colors.Steel4,
            strokeColor = Colors.White,
            fontStyle = FontStyle.Italic,
            style = style.copy(fontSize = style.fontSize * 1.5f, brush = ResumeNumberBrush),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }

    @Composable
    private fun GameOverlayLoading() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v5, Alignment.CenterVertically)
        ) {
            CircleLoading.Content(modifier = Modifier.size(Theme.size.image8))
            SimpleClipText(text = "在线加载资源中...", style = Theme.typography.v4.bold)
        }
    }

    @Composable
    private fun GameOverlayStart() {
        Box(modifier = Modifier.fillMaxSize()) {
            LocalFileImage(
                uri = prologueBackground?.toString() ?: "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(Theme.padding.value7),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RhymeAcrylicButton(icon = Icons.ArrowBack, onClick = ::onBack)
                }
                Row(
                    modifier = Modifier.padding(Theme.padding.value7),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RhymeAcrylicButton(icon = Icons.MusicNote, onClick = { state = GameState.MusicLibrary })
                }
            }
        }
    }

    @Composable
    private fun GameOverlayMusicLibrary() {
        RhymeOverlayLayout(
            title = "曲库",
            onBack = ::onBack,
            action = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = showEnabled, onCheckedChange = { showEnabled = it })
                    SimpleEllipsisText(text = "${if (showEnabled) "已" else "未"}解锁", style = Theme.typography.v6.bold)
                }
            }
        ) {
            val showLibrary = remember(showEnabled) {
                if (showEnabled) library.filter { it.enabled } else library
            }

            if (showLibrary.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SimpleClipText(text = "曲库中无支持的音乐MOD", style = Theme.typography.v4.bold)
                }
            }
            else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Theme.size.image5),
                    contentPadding = Theme.padding.eValue9,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.e9),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e9),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = showLibrary,
                        key = { it.musicInfo.id }
                    ) {
                        RhymeMusicCard(
                            entry = it,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { state = GameState.MusicDetails(it) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun GameOverlayMusicDetails(entry: RhymeMusic) {
        var difficulty by rememberState { RhymePlayConfig.Default.difficulty }
        var audioDelay by rememberState { SliderArgs(0L, RhymePlayConfig.MIN_AUDIO_DELAY, RhymePlayConfig.MAX_AUDIO_DELAY) }

        RhymeOverlayLayout(
            title = entry.musicInfo.name,
            onBack = ::onBack,
            action = {
                RhymeButton(
                    icon = Icons.PlayArrow,
                    onClick = {
                        if (entry.enabled) startGame(
                            info = entry.musicInfo,
                            playConfig = RhymePlayConfig(
                                difficulty = difficulty,
                                audioDelay = audioDelay.value
                            )
                        )
                        else slot.tip.warning("此MOD不支持")
                    }
                )
            }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue9),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h5),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v5),
                ) {
                    SimpleEllipsisText(text = "难度", style = Theme.typography.v6.bold)

                    val difficulties = RhymeDifficulty.entries
                    Filter(
                        size = difficulties.size,
                        selectedProvider = { difficulty == difficulties[it] },
                        titleProvider = { difficulties[it].title },
                        onClick = { index, selected -> if (selected) difficulty = difficulties[index] }
                    )
                    ArgsSlider(
                        title = "延迟补偿(毫秒)",
                        args = audioDelay,
                        onValueChange = { audioDelay = audioDelay.copy(tmpValue = it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    SimpleEllipsisText(text = "排行榜", style = Theme.typography.v6.bold)
                }
            }
        }
    }

    @Composable
    private fun GameOverlayPlaying() {
        val rhymeFont = rememberFontFamily(Res.font.rhyme)

        LocalFileImage(
            uri = prologueBackground?.toString() ?: "",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(Colors.Black).zIndex(1f),
            alpha = 0.9f
        )

        rhymeManager.SceneContent(
            modifier = Modifier.fillMaxSize().zIndex(2f),
            fonts = listOf(mainFont(), rhymeFont)
        )
    }

    @Composable
    private fun GameOverlaySettling(result: RhymeResult) {
        RhymeOverlayLayout(
            title = "结算",
            onBack = ::onBack,
            action = {
                RhymeButton(
                    icon = Icons.CloudUpload,
                    onClick = { slot.tip.info("记录上传功能敬请期待") }
                )
            }
        ) {
            Column {
                Text(text = "音游仍在内测中, 结算页尚未完成设计, 仅供参考, 感谢支持")
                Text(text = "难度: ${result.playConfig.difficulty.title}")
                Text(text = "得分: ${result.playResult.score}")
                ActionResult.entries.fastForEachIndexed { index, actionResult ->
                    Text(text = "${actionResult.title}: ${result.playResult.statistics[index]}")
                }
            }
        }
    }

    override val title: String? = null

    override suspend fun initialize() {
        rhymeManager.init()
        if (rhymeManager.isInit) coroutineScope {
            val count = atomic(0)
            val tasks = arrayOf(
                async(ioContext) {
                    catching { // 下载开屏背景图
                        prologueBackground = NetClient.downloadCacheWithPath(ServerRes.Game.Rhyme.res("prologue.webp").url)
                        count.incrementAndGet()
                    }
                },
                async(ioContext) {
                    catching { // 从服务器下载资源文件
                        rhymeManager.run { downloadAssets() }
                        count.incrementAndGet()
                    }
                },
                async(ioContext) {
                    catching { // 检查包含游戏配置文件的 MOD
                        library = app.startup<StartupMusicPlayer>()?.library?.values?.map { info ->
                            RhymeMusic(
                                musicInfo = info,
                                enabled = info.path(PathMod, ModResourceType.Rhyme).exists
                            )
                        } ?: emptyList()
                        count.incrementAndGet()
                    }
                }
            )
            awaitAll(*tasks)
            if (count.value == tasks.size) state = GameState.Start
            else slot.tip.warning("下载资源失败")
        }
    }

    override fun finalize() {
        rhymeManager.release()
    }

    override fun onBack() {
        if (lockState is GameLockState.Normal) {
            when (state) {
                is GameState.Loading, is GameState.Start -> pop()
                is GameState.MusicLibrary -> state = GameState.Start
                is GameState.MusicDetails -> state = GameState.MusicLibrary
                is GameState.Playing -> pauseGame(GameLockState.Pause)
                is GameState.Settling -> state = GameState.Start
            }
        }
    }

    @Composable
    override fun Content() {
        val deviceType = LocalDevice.current.type
        LaunchedEffect(deviceType) {
            // 监听屏幕朝向变化
            onScreenOrientationChanged(deviceType)
        }

        // 首次打开切换横屏
        LaunchedEffect(Unit) {
            orientationStarter {
                orientationController.orientation = Orientation.Landscape
            }
        }

        OffScreenEffect { isForeground ->
            // 离屏且处于游戏状态时自动暂停
            if (!isForeground && state is GameState.Playing) {
                pauseGame(GameLockState.Pause)
            }
        }

        Layout(
            modifier = Modifier.background(Colors.Black).fillMaxSize().background(Theme.color.background),
            content = {
                // 状态层
                AnimationContent(state = state, modifier = Modifier.fillMaxSize()) {
                    when (it) {
                        is GameState.Loading -> GameOverlayLoading()
                        is GameState.Start -> GameOverlayStart()
                        is GameState.MusicLibrary -> GameOverlayMusicLibrary()
                        is GameState.MusicDetails -> GameOverlayMusicDetails(it.entry)
                        is GameState.Playing -> GameOverlayPlaying()
                        is GameState.Settling -> GameOverlaySettling(it.result)
                    }
                }

                // 遮罩层
                val currentLockState = lockState
                if (currentLockState !is GameLockState.Normal && state != GameState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Theme.color.scrim.copy(alpha = RhymeConfig.LOCK_SCRIM_ALPHA))
                            .zIndex(2f)
                            .silentClick { },
                        contentAlignment = Alignment.Center
                    ) {
                        when (currentLockState) {
                            is GameLockState.PortraitLock -> GameMaskPortraitLock()
                            is GameLockState.Pause -> GameMaskPause()
                            is GameLockState.Resume -> GameMaskResume(currentLockState)
                        }
                    }
                }
            },
            measurePolicy = { measurables, constraints ->
                val maxWidth = constraints.maxWidth
                val maxHeight = constraints.maxHeight
                val useWidthFirst = maxWidth * 9 <= maxHeight * 16
                val childWidth = if (useWidthFirst) maxWidth else maxHeight * 16 / 9
                val childHeight = if (useWidthFirst) maxWidth * 9 / 16 else maxHeight
                val childConstraints = Constraints.fixed(childWidth, childHeight)
                val placeables = measurables.map { it.measure(childConstraints) }
                layout(childWidth, childHeight) {
                    placeables.forEach { it.placeRelative(0, 0) }
                }
            }
        )
    }
}