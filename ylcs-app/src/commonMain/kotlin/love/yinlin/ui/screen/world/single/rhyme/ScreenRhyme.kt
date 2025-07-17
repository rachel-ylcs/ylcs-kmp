package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ThemeStyle
import love.yinlin.common.ThemeValue
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.data.rachel.game.Game
import love.yinlin.extension.OffScreenEffect
import love.yinlin.extension.catchingNull
import love.yinlin.extension.launchFlag
import love.yinlin.extension.mutableRefStateOf
import love.yinlin.extension.parseJsonValue
import love.yinlin.platform.Coroutines
import love.yinlin.platform.MusicPlayer
import love.yinlin.platform.app
import love.yinlin.ui.component.animation.AnimationLayout
import love.yinlin.ui.component.image.LoadingCircle
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.Switch
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.layout.SplitLayout
import love.yinlin.ui.component.node.clickableNoRipple
import love.yinlin.ui.component.platform.Orientation
import love.yinlin.ui.component.platform.rememberOrientationController
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.text.StrokeText
import love.yinlin.ui.screen.music.audioPath
import love.yinlin.ui.screen.music.rhymePath
import kotlin.time.Duration.Companion.milliseconds

@Stable
class ScreenRhyme(model: AppModel) : CommonSubScreen(model) {
    private var state: GameState by mutableRefStateOf(GameState.Loading)
    private var lockState: GameLockState by mutableRefStateOf(GameLockState.Normal)

    private var library = emptyList<RhymeMusic>()
    private var showEnabled by mutableStateOf(false)

    private val musicPlayer = MusicPlayer()
    private val stage = RhymeStage()

    private var canvasFrameJob: Job? = null
    private var resumePauseJob: Job? = null

    private val orientationStarter = launchFlag()

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

    private fun monitorGamePosition(): Job = launch {
        val delayDuration = (1000 / RhymeConfig.FPS).milliseconds
        while (true) {
            stage.update(musicPlayer.position)
            delay(delayDuration)
        }
    }

    private fun startGame(info: MusicInfo) {
        launch {
            val lyrics = Coroutines.io {
                catchingNull { SystemFileSystem.source(info.rhymePath).buffered().readString().parseJsonValue<RhymeLyricsConfig>() }
            }
            if (lyrics != null && canvasFrameJob == null) {
                musicPlayer.load(info.audioPath)
                stage.init(lyrics)
                canvasFrameJob = monitorGamePosition()
                state = GameState.Playing
            }
        }
    }

    private fun stopGame() {
        musicPlayer.stop()
        canvasFrameJob?.cancel()
        canvasFrameJob = null
        stage.clear()
        state = GameState.Start
    }

    private fun pauseGame(newState: GameLockState) {
        resumePauseJob?.cancel()
        resumePauseJob = null
        if (state is GameState.Playing) {
            musicPlayer.pause()
            canvasFrameJob?.cancel()
            canvasFrameJob = null
        }
        lockState = newState
    }

    private fun resumePauseTimer() {
        if (resumePauseJob == null) {
            resumePauseJob = launch {
                try {
                    // 倒计时解除暂停状态
                    repeat(RhymeConfig.PAUSE_TIME) {
                        lockState = GameLockState.Resume(RhymeConfig.PAUSE_TIME - it)
                        delay(1000L)
                    }
                    lockState = GameLockState.Normal
                    musicPlayer.play()
                    if (canvasFrameJob == null) canvasFrameJob = monitorGamePosition()
                }
                finally {
                    resumePauseJob = null
                }
            }
        }
    }

    @Composable
    private fun GameMaskPortraitLock() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace * 2)
        ) {
            Box(contentAlignment = Alignment.Center) {
                LoadingCircle(
                    size = ThemeValue.Size.Image * 1.25f,
                    color = Colors.White,
                    modifier = Modifier.zIndex(1f)
                )
                MiniIcon(
                    icon = Icons.Outlined.Lock,
                    color = Colors.White,
                    size = ThemeValue.Size.Image,
                    modifier = Modifier.zIndex(2f)
                )
            }
            StrokeText(
                text = "请保持横屏",
                color = Colors.Steel4,
                strokeColor = Colors.White,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }

    @Composable
    private fun GameMaskPause() {
        Column(
            modifier = Modifier
                .padding(ThemeValue.Padding.HorizontalExtraSpace * 2)
                .shadow(ThemeValue.Shadow.Surface, MaterialTheme.shapes.extraLarge)
                .background(Colors.Gray8, MaterialTheme.shapes.extraLarge)
                .padding(
                    horizontal = ThemeValue.Padding.HorizontalExtraSpace * 4,
                    vertical = ThemeValue.Padding.VerticalExtraSpace * 2
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace * 4)
        ) {
            Text(
                text = "暂停中",
                color = Colors.White,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RhymeButton(
                    icon = Icons.Outlined.Close,
                    transparent = false,
                    onClick = {
                        lockState = GameLockState.Normal
                        stopGame()
                    }
                )
                RhymeButton(
                    icon = Icons.Outlined.PlayArrow,
                    transparent = false,
                    onClick = { resumePauseTimer() }
                )
            }
        }
    }

    @Composable
    private fun GameMaskResume(resumeState: GameLockState.Resume) {
        StrokeText(
            text = remember(resumeState) { resumeState.time.toString() },
            color = Colors.Steel4,
            strokeColor = Colors.White,
            fontStyle = FontStyle.Italic,
            style = ThemeStyle.RhymeDisplay.copy(
                brush = Brush.linearGradient(listOf(Colors.Steel4, Colors.Blue4, Colors.Purple4))
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }

    @Composable
    private fun GameOverlayLoading() {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadingBox(
                text = "加载曲库中...",
                color = Colors.White
            )
        }
    }

    @Composable
    private fun GameOverlayStart() {
        Box(modifier = Modifier.fillMaxSize()) {
            WebImage(
                uri = Game.Rhyme.zPath,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
            SplitLayout(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue).zIndex(2f),
                horizontalArrangement = ThemeValue.Padding.HorizontalExtraSpace,
                verticalAlignment = Alignment.CenterVertically,
                left = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RhymeButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            onClick = { onBack() }
                        )
                    }
                },
                right = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RhymeButton(
                            icon = Icons.Outlined.MusicNote,
                            onClick = { state = GameState.MusicLibrary }
                        )
                    }
                }
            )
        }
    }

    @Composable
    private fun GameOverlayMusicLibrary() {
        RhymeOverlayLayout(
            title = "曲库",
            onBack = ::onBack,
            action = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = showEnabled,
                        onCheckedChange = { showEnabled = it }
                    )
                    Text(
                        text = "${if (showEnabled) "已" else "未"}解锁",
                        style = MaterialTheme.typography.titleMedium,
                        color = Colors.White,
                    )
                }
            }
        ) {
            val showLibrary = remember(library, showEnabled) {
                if (showEnabled) library.filter { it.enabled } else library
            }

            if (showLibrary.isEmpty()) {
                EmptyBox(
                    text = "曲库中无支持的音乐MOD",
                    color = Colors.White
                )
            }
            else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(ThemeValue.Size.LargeImage),
                    contentPadding = PaddingValues(horizontal = ThemeValue.Padding.EqualSpace),
                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
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
        RhymeOverlayLayout(
            title = entry.musicInfo.name,
            onBack = ::onBack,
            action = {
                RhymeButton(
                    icon = Icons.Outlined.PlayArrow,
                    transparent = false,
                    onClick = {
                        if (entry.enabled) startGame(entry.musicInfo)
                        else slot.tip.warning("此MOD不支持")
                    }
                )
            }
        ) {

        }
    }

    @Composable
    private fun GameOverlayPlaying() {
        Box(modifier = Modifier.fillMaxSize()) {

        }
    }

    @Composable
    private fun GameOverlaySettling() {

    }

    @Composable
    private fun GameScrimMask(modifier: Modifier) {
        Box(modifier = modifier) {
            val currentLockState = lockState
            if (currentLockState !is GameLockState.Normal && state != GameState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = RhymeConfig.LOCK_SCRIM_ALPHA))
                        .clickableNoRipple { },
                    contentAlignment = Alignment.Center
                ) {
                    when (currentLockState) {
                        is GameLockState.Normal -> {}
                        is GameLockState.PortraitLock -> GameMaskPortraitLock()
                        is GameLockState.Pause -> GameMaskPause()
                        is GameLockState.Resume -> GameMaskResume(currentLockState)
                    }
                }
            }
        }
    }

    @Composable
    private fun GameOverlay(modifier: Modifier) {
        Box(modifier = modifier) {
            AnimationLayout(state = state) {
                when (it) {
                    is GameState.Loading -> GameOverlayLoading()
                    is GameState.Start -> GameOverlayStart()
                    is GameState.MusicLibrary -> GameOverlayMusicLibrary()
                    is GameState.MusicDetails -> GameOverlayMusicDetails(it.entry)
                    is GameState.Playing -> GameOverlayPlaying()
                    is GameState.Settling -> GameOverlaySettling()
                }
            }
        }
    }

    @Composable
    private fun GameCanvas(modifier: Modifier) {
        if (state is GameState.Playing) {
            Canvas(modifier = modifier) {
                val scope = RhymeDrawScope(scope = this, scale = size.width / 1920)
                with(stage) { scope.draw() }
            }
        }
    }

    @Composable
    private fun GameBackground(modifier: Modifier) {
        if (state is GameState.Playing) {
            Box(modifier = modifier)
        }
    }

    override val title: String? = null

    override suspend fun initialize() {
        if (app.musicFactory.isInit) {
            coroutineScope {
                val task1 = async {
                    musicPlayer.init()
                }
                val task2 = async {
                    Coroutines.io {
                        // 检查包含游戏配置文件的 MOD
                        library = app.musicFactory.musicLibrary.values.map { info ->
                            RhymeMusic(
                                musicInfo = info,
                                enabled = SystemFileSystem.metadataOrNull(info.rhymePath)?.isRegularFile == true
                            )
                        }
                    }
                }
                task1.await()
                task2.await()
            }
        }
        state = GameState.Start
    }

    override fun finalize() {
        musicPlayer.release()
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
    override fun SubContent(device: Device) {
        LaunchedEffect(device.type) {
            // 监听屏幕朝向变化
            onScreenOrientationChanged(device.type)
        }

        // 首次打开切换横屏
        val controller = rememberOrientationController()
        orientationStarter {
            controller.orientation = Orientation.LANDSCAPE
        }

        OffScreenEffect { isForeground ->
            // 离屏且处于游戏状态时自动暂停
            if (!isForeground && state is GameState.Playing) {
                pauseGame(GameLockState.Pause)
            }
        }

        Layout(
            modifier = Modifier
                .background(Colors.Black)
                .fillMaxSize()
                .background(Colors.Dark),
            content = {
                // 遮罩层
                GameScrimMask(modifier = Modifier.fillMaxSize().zIndex(4f))
                // 状态层
                GameOverlay(modifier = Modifier.fillMaxSize().zIndex(3f))
                // 画布层
                GameCanvas(modifier = Modifier.fillMaxSize().zIndex(2f))
                // 背景层
                GameBackground(modifier = Modifier.fillMaxSize().zIndex(1f))
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