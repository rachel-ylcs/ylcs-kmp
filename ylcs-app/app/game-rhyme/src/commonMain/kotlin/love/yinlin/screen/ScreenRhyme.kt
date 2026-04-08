package love.yinlin.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import love.yinlin.app
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.data.RhymePlayResult
import love.yinlin.compose.game.data.RhymeState
import love.yinlin.compose.game.viewport.Viewport
import love.yinlin.compose.game.plugin.AssetPlugin
import love.yinlin.compose.game.plugin.FontPlugin
import love.yinlin.compose.game.plugin.RhymePlugin
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.plugin.SoundPlugin
import love.yinlin.compose.game.ui.RhymeCommonButton
import love.yinlin.compose.game.ui.RhymeMusicCard
import love.yinlin.compose.game.viewport.Camera
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.animation.WaveLoading
import love.yinlin.compose.ui.common.ArgsSlider
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.common.value
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.catchingError
import love.yinlin.extension.parseJsonValue
import love.yinlin.startup.StartupMusicPlayer
import org.jetbrains.compose.resources.Font
import kotlin.time.Duration.Companion.seconds

@Stable
class ScreenRhyme : BasicScreen() {
    private val engine = Engine(
        viewport = Viewport.MatchHeight(2000),
        backgroundColor = Colors.Black,
        FontPlugin.ResourceFactory(
            GlobalRes.font.xwwk,
            RhymeRes.font.rhyme,
        ),
        AssetPlugin.Factory(),
        ScenePlugin.Factory(
            fpsRate = 0L,
            cameraConfig = Camera.Config(
                moveSmoothness = 1000f,
                scaleSmoothness = 4f
            )
        ),
        SoundPlugin.Factory(listOf()),
        RhymePlugin.Factory(
            context = app.rawContext,
            endListener = ::endGame
        )
    )

    private var gameState: RhymeState by mutableStateOf(RhymeState.Start)
    private var gameError: Boolean by mutableStateOf(false)

    private val library = mutableListOf<MusicInfo>()

    private fun startGame(info: MusicInfo, playConfig: RhymePlayConfig) {
        launch {
            catchingError {
                val modPath = app.modPath
                // 解析歌词文件
                val lyricsText = info.path(modPath, ModResourceType.Rhyme).readText()
                require(lyricsText != null) { "歌词资源文件丢失或损坏" }
                val lyricsConfig = lyricsText.parseJsonValue<RhymeLyricsConfig>()
                // 解析封面图片
                val recordImage = info.path(modPath, ModResourceType.Record).readByteArray()?.let { ImageBitmap.decode(it) }
                require(recordImage != null) { "封面资源文件丢失" }
                require(lyricsConfig.id == info.id) { "歌词资源文件与MOD不匹配" }
                // 音频路径
                val audio = info.path(modPath, ModResourceType.Audio)

                engine.plugin<RhymePlugin>().setupGame(
                    playInfo = RhymePlayInfo(
                        playConfig = playConfig,
                        musicInfo = info,
                        lyricsConfig = lyricsConfig,
                        musicRecord = recordImage
                    ),
                    audio = audio
                )
                engine.isRunning = true
                gameState = RhymeState.Playing(info, playConfig)
            }.errorTip
        }
    }

    private fun endGame(result: RhymePlayResult?) {
        engine.isRunning = false
        gameState = if (result == null) RhymeState.Start else {
            val state = gameState
            if (state is RhymeState.Playing) RhymeState.Settling(state.info, state.playConfig, result) else RhymeState.Start
        }
    }

    override suspend fun initialize() {
        // 初始化曲库
        app.requireClassOrNull<StartupMusicPlayer>()?.library?.values?.let { rawLibrary ->
            Coroutines.io {
                val modPath = app.modPath
                rawLibrary.mapNotNullTo(library) { info ->
                    if (info.path(modPath, ModResourceType.Rhyme).exists()) info else null
                }
            }
        }
        // 初始化游戏引擎
        if (!engine.initialize()) gameError = true
    }

    override fun finalize() {
        engine.release()
    }

    override fun onBack() {
        when (gameState) {
            is RhymeState.Start -> super.onBack()
            is RhymeState.Playing -> engine.isRunning = false
            else -> gameState = RhymeState.Start
        }
    }

    @Composable
    private fun GameStartLayout() {
        val rhymeFont = FontFamily(Font(RhymeRes.font.rhyme))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v1, Alignment.CenterVertically)
        ) {
            SimpleClipText(text = "Rhyme", style = Theme.typography.v1.bold.copy(fontFamily = rhymeFont))

            val rotateColors = remember { listOf(Colors.Green4, Colors.Red4, Colors.Orange4, Colors.Pink4, Colors.Purple4) }
            val rotateValues = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(1.seconds)
                    val (index1, index2) = rotateValues.indices.shuffled().take(2)
                    rotateValues[index1] += 45f
                    rotateValues[index2] -= 45f
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(rotateValues.size) { index ->
                    val angle by animateFloatAsState(rotateValues[index])
                    Box(modifier = Modifier.size(Theme.size.icon).drawWithContent {
                        rotate(angle) {
                            drawRect(rotateColors[index])
                            drawRect(Colors.White, style = Stroke(2f))
                        }
                    })
                }
            }

            Column(
                modifier = Modifier.width(Theme.size.cell1),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v8)
            ) {
                if (engine.isInitialized) {
                    RhymeCommonButton(icon = Icons.LibraryMusic, text = "曲库", onClick = { gameState = RhymeState.MusicLibrary }, modifier = Modifier.fillMaxWidth())
                    RhymeCommonButton(icon = Icons.RewardCup, text = "排行榜", onClick = { gameState = RhymeState.Rank }, modifier = Modifier.fillMaxWidth())
                }
                else if (gameError) {
                    SimpleClipText(text = "引擎加载失败", color = Theme.color.error, style = Theme.typography.v5.bold)
                }
                else {
                    WaveLoading.Content()
                    SimpleClipText(text = "正在加载中...", style = Theme.typography.v5.bold)
                }
                RhymeCommonButton(icon = Icons.ArrowBack, text = "返回", onClick = ::onBack, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun GameMusicLibraryLayout() {
        Column(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleClipText(text = "曲库", style = Theme.typography.v4.bold, modifier = Modifier.padding(Theme.padding.value7))
                ActionScope.Right.Container(modifier = Modifier.weight(1f).padding(Theme.padding.value9)) {
                    Icon(icon = Icons.ArrowBack, tip = "返回", onClick = ::onBack)
                }
            }

            if (library.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SimpleClipText(text = "曲库MOD未下载游戏配置", style = Theme.typography.v5.bold)
                }
            }
            else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Theme.size.cell1),
                    contentPadding = Theme.padding.eValue10,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.e10),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e10),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(items = library, key = { it.id }) { info ->
                        RhymeMusicCard(
                            info = info,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { gameState = RhymeState.Prepare(info) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun GamePrepareLayout(info: MusicInfo) {
        var difficulty by rememberState { RhymePlayConfig.Default.difficulty }
        var audioDelay by rememberState { SliderArgs(0L, RhymePlayConfig.MIN_AUDIO_DELAY, RhymePlayConfig.MAX_AUDIO_DELAY) }

        Column(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleClipText(text = "准备", style = Theme.typography.v4.bold, modifier = Modifier.padding(Theme.padding.value7))
                ActionScope.Right.Container(modifier = Modifier.weight(1f).padding(Theme.padding.value9)) {
                    Icon(icon = Icons.ArrowBack, tip = "返回", onClick = ::onBack)
                    Icon(icon = Icons.PlayArrow, tip = "开始", onClick = {
                        startGame(info, RhymePlayConfig(
                            difficulty = difficulty,
                            audioDelay = audioDelay.value
                        ))
                    })
                }
            }

            Column(
                modifier = Modifier.width(Theme.size.cell1).padding(Theme.padding.value9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                RhymeMusicCard(info = info, modifier = Modifier.fillMaxWidth())

                SimpleClipText(text = "难度", style = Theme.typography.v6.bold)

                Filter(
                    size = RhymeDifficulty.entries.size,
                    selectedProvider = { difficulty == RhymeDifficulty.entries[it] },
                    titleProvider = { RhymeDifficulty.entries[it].title },
                    onClick = { index, selected -> if (selected) difficulty = RhymeDifficulty.entries[index] }
                )

                ArgsSlider(
                    title = "延迟补偿(毫秒)",
                    args = audioDelay,
                    onValueChange = { audioDelay = audioDelay.copy(tmpValue = it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun GameSettlingLayout() {

    }

    @Composable
    private fun GameRankLayout() {

    }

    @Composable
    override fun BasicContent() {
        Theme.ThemeModeWrapper(true) {
            AnimationContent(gameState, modifier = Modifier.fillMaxSize().background(Theme.color.background)) { state ->
                when (state) {
                    is RhymeState.Start -> GameStartLayout()
                    is RhymeState.MusicLibrary -> GameMusicLibraryLayout()
                    is RhymeState.Prepare -> GamePrepareLayout(state.info)
                    is RhymeState.Playing -> engine.ViewportContent(modifier = Modifier.fillMaxSize())
                    is RhymeState.Settling -> GameSettlingLayout()
                    is RhymeState.Rank -> GameRankLayout()
                }
            }
        }
    }
}