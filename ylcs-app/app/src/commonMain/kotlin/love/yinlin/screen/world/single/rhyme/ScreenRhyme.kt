package love.yinlin.screen.world.single.rhyme

import androidx.collection.mutableLongObjectMapOf
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
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
import kotlinx.io.readByteArray
import kotlinx.io.readString
import love.yinlin.app
import love.yinlin.common.Paths
import love.yinlin.common.Shaders
import love.yinlin.compose.*
import love.yinlin.compose.graphics.ShaderBox
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.data.rachel.game.Game
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.platform.AudioPlayer
import love.yinlin.platform.Coroutines
import love.yinlin.resources.*
import love.yinlin.compose.ui.animation.AnimationLayout
import love.yinlin.compose.ui.image.LoadingCircle
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.LoadingBox
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.compose.ui.node.clickableNoRipple
import love.yinlin.compose.ui.text.StrokeText
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.catching
import love.yinlin.extension.exists
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.time.Duration.Companion.milliseconds

@Stable
class ScreenRhyme(manager: ScreenManager) : Screen(manager) {
    private var state: GameState by mutableRefStateOf(GameState.Loading)
    private var lockState: GameLockState by mutableRefStateOf(GameLockState.Normal)

    private var library = emptyList<RhymeMusic>()
    private var showEnabled by mutableStateOf(false)

    private val musicPlayer = AudioPlayer(app.context)
    private val stage = RhymeStage()

    private var canvasFrameJob: Job? = null
    private var resumePauseJob: Job? = null

    private val orientationStarter = LaunchFlag()

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
        var lastPosition = 0L
        while (true) {
            val position = musicPlayer.position
            if (position == 0L && lastPosition != 0L) break
            lastPosition = position
            stage.onUpdate(position)
            delay(delayDuration)
        }
        completeGame()
    }

    private fun startGame(info: MusicInfo) {
        launch {
            val task1 = async {
                Coroutines.io {
                    catchingNull {
                        SystemFileSystem.source(info.path(Paths.modPath, ModResourceType.Rhyme)).buffered().use {
                            it.readString().parseJsonValue<RhymeLyricsConfig>()
                        }
                    }
                }
            }
            val task2 = async {
                Coroutines.io {
                    catchingNull {
                        val environment = getSystemResourceEnvironment()
                        ImageSet(
                            record = SystemFileSystem.source(info.path(Paths.modPath, ModResourceType.Record)).buffered().use { it.readByteArray().decodeToImageBitmap() },
                            noteLayoutMap = getDrawableResourceBytes(environment, Res.drawable.note_map).decodeToImageBitmap(),
                            clickAnimationNote = getDrawableResourceBytes(environment, Res.drawable.click_animation_note).decodeToImageBitmap(),
                        )
                    }
                }
            }
            val lyrics = task1.await()
            val imageSet = task2.await()
            if (lyrics != null && imageSet != null && canvasFrameJob == null) {
                stage.onInitialize(lyrics, imageSet)
                musicPlayer.load(info.path(Paths.modPath, ModResourceType.Audio))
                canvasFrameJob = monitorGamePosition()
                state = GameState.Playing
            }
        }
    }

    private fun stopGame() {
        musicPlayer.stop()
        canvasFrameJob?.cancel()
        canvasFrameJob = null
        stage.onClear()
        state = GameState.Start
    }

    private fun completeGame() {
        canvasFrameJob = null
        val result = stage.onResult()
        stage.onClear()
        state = GameState.Settling(result)
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
                catching {
                    // 倒计时解除暂停状态
                    repeat(RhymeConfig.PAUSE_TIME) {
                        lockState = GameLockState.Resume(RhymeConfig.PAUSE_TIME - it)
                        delay(1000L)
                    }
                    lockState = GameLockState.Normal
                    musicPlayer.play()
                    if (canvasFrameJob == null) canvasFrameJob = monitorGamePosition()
                }
                resumePauseJob = null
            }
        }
    }

    @Composable
    private fun GameMaskPortraitLock() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace * 2)
        ) {
            Box(contentAlignment = Alignment.Center) {
                LoadingCircle(
                    size = CustomTheme.size.image * 1.25f,
                    color = Colors.White,
                    modifier = Modifier.zIndex(1f)
                )
                MiniIcon(
                    icon = Icons.Outlined.Lock,
                    color = Colors.White,
                    size = CustomTheme.size.image,
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
                .padding(CustomTheme.padding.horizontalExtraSpace * 2)
                .shadow(CustomTheme.shadow.surface, MaterialTheme.shapes.extraLarge)
                .background(Colors.Gray8, MaterialTheme.shapes.extraLarge)
                .padding(
                    horizontal = CustomTheme.padding.horizontalExtraSpace * 4,
                    vertical = CustomTheme.padding.verticalExtraSpace * 2
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace * 4)
        ) {
            Text(
                text = "暂停中",
                color = Colors.White,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
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
            style = CustomTheme.typography.rhymeDisplay.copy(
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
                uri = remember { Game.Rhyme.resPath("start") },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
            SplitLayout(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue).zIndex(2f),
                horizontalArrangement = CustomTheme.padding.horizontalExtraSpace,
                verticalAlignment = Alignment.CenterVertically,
                left = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace, Alignment.Start),
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
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace, Alignment.End),
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
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
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
                    columns = GridCells.Adaptive(CustomTheme.size.largeImage),
                    contentPadding = PaddingValues(horizontal = CustomTheme.padding.equalSpace),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
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

    }

    @Composable
    private fun GameOverlaySettling() {

    }

    @Composable
    private fun GameBackground() {
        if (state is GameState.Playing) {
            ShaderBox(Shaders.GradientFlow, modifier = Modifier.fillMaxSize()) {
                WebImage(
                    uri = remember { Game.Rhyme.resPath("background") },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @Composable
    private fun GameCanvas() {
        if (state is GameState.Playing) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val rhymeScale = with(LocalDensity.current) { maxWidth.toPx() } / 1920
                val fontFamilyResolver = LocalFontFamilyResolver.current
                val font = mainFont()
                val textManager = remember(font, fontFamilyResolver) { RhymeTextManager(font, fontFamilyResolver) }

                val pointers = remember { mutableLongObjectMapOf<Pointer>() }

                Canvas(modifier = Modifier.fillMaxSize().clipToBounds().pointerInput(rhymeScale) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            for (change in event.changes) {
                                val id = change.id.value
                                val position = change.position / rhymeScale
                                val time = musicPlayer.position
                                when {
                                    change.changedToDown() -> Pointer(id = id, position = position, startTime = time).let { pointer ->
                                        pointers[id] = pointer
                                        stage.onEvent(pointer)
                                    }
                                    change.changedToUp() -> pointers.remove(id)?.let { pointer ->
                                        stage.onEvent(pointer.copy(endTime = time))
                                    }
                                }
                            }
                        }
                    }
                }) {
                    scale(scale = rhymeScale, pivot = Offset.Zero) {
                        stage.onDraw(scope = this, textManager = textManager)
                    }
                }
            }
        }
    }

    @Composable
    private fun GameOverlay() {
        AnimationLayout(
            state = state,
            modifier = Modifier.fillMaxSize()
        ) {
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

    @Composable
    private fun GameScrimMask() {
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

    override val title: String? = null

    override suspend fun initialize() {
        if (app.mp.isInit) {
            coroutineScope {
                val task1 = async {
                    musicPlayer.init()
                }
                val task2 = async {
                    Coroutines.io {
                        // 检查包含游戏配置文件的 MOD
                        library = app.mp.library.values.map { info ->
                            RhymeMusic(
                                musicInfo = info,
                                enabled = info.path(Paths.modPath, ModResourceType.Rhyme).exists
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
    override fun Content(device: Device) {
        LaunchedEffect(device.type) {
            // 监听屏幕朝向变化
            onScreenOrientationChanged(device.type)
        }

        // 首次打开切换横屏
        val controller = rememberOrientationController()
        LaunchedEffect(controller) {
            orientationStarter {
                controller.orientation = Orientation.LANDSCAPE
            }
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
                GameBackground() // 背景层
                GameCanvas() // 画布层
                GameOverlay() // 状态层
                GameScrimMask() // 遮罩层
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