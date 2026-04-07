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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import love.yinlin.app
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.app.global.resources.img_logo
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.asset.ResourceImageLoader
import love.yinlin.compose.game.data.GameState
import love.yinlin.compose.game.viewport.Viewport
import love.yinlin.compose.game.plugin.AssetPlugin
import love.yinlin.compose.game.plugin.FontPlugin
import love.yinlin.compose.game.plugin.MusicLibraryPlugin
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.ui.GameCommonButton
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.animation.WaveLoading
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.startup.StartupMusicPlayer
import org.jetbrains.compose.resources.Font
import kotlin.time.Duration.Companion.seconds

@Stable
class ScreenRhyme : BasicScreen() {
    private val engine = Engine(
        viewport = Viewport.MatchHeight(1000),
        backgroundColor = Colors.Black,
        FontPlugin.ResourceFactory(
            GlobalRes.font.xwwk,
            RhymeRes.font.rhyme,
        ),
        AssetPlugin.Factory(
            ResourceImageLoader(
                GlobalRes.drawable.img_logo
            )
        ),
        ScenePlugin.Factory(),
        MusicLibraryPlugin.Factory(app.modPath, app.requireClassOrNull<StartupMusicPlayer>()?.library?.values)
    )

    private var gameState: GameState by mutableStateOf(GameState.Start)
    private var gameError: Boolean by mutableStateOf(false)

    override suspend fun initialize() {
        if (!engine.initialize()) gameError = true
    }

    override fun finalize() {
        engine.release()
    }

    override fun onBack() {
        when (gameState) {
            is GameState.Start -> super.onBack()
            else -> gameState = GameState.Start
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
                    GameCommonButton(icon = Icons.LibraryMusic, text = "曲库", onClick = { gameState = GameState.MusicLibrary }, modifier = Modifier.fillMaxWidth())
                    GameCommonButton(icon = Icons.RewardCup, text = "排行榜", onClick = { gameState = GameState.Rank }, modifier = Modifier.fillMaxWidth())
                }
                else if (gameError) {
                    SimpleClipText(text = "引擎加载失败", color = Theme.color.error, style = Theme.typography.v5.bold)
                }
                else {
                    WaveLoading.Content()
                    SimpleClipText(text = "正在加载中...", style = Theme.typography.v5.bold)
                }
                GameCommonButton(icon = Icons.ArrowBack, text = "返回", onClick = ::onBack, modifier = Modifier.fillMaxWidth())
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

            val library: List<MusicInfo> = remember { engine.plugin<MusicLibraryPlugin>().library }
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
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = Theme.shadow.v5,
                            tonalLevel = 1,
                            shape = Theme.shape.v7,
                            onClick = { gameState = GameState.Prepare(info) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                            ) {
                                LocalFileImage(
                                    uri = info.path(app.modPath, ModResourceType.Record).path,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(Theme.size.image7)
                                )
                                Column(modifier = Modifier.weight(1f).padding(Theme.padding.value)) {
                                    SimpleEllipsisText(
                                        text = info.name,
                                        textAlign = TextAlign.Center,
                                        style = Theme.typography.v6.bold,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GamePrepareLayout(info: MusicInfo) {

    }

    @Composable
    private fun GameRankLayout() {

    }

    @Composable
    override fun BasicContent() {
        Theme.ThemeModeWrapper(true) {
            AnimationContent(gameState, modifier = Modifier.fillMaxSize().background(Theme.color.background)) { state ->
                when (state) {
                    is GameState.Start -> GameStartLayout()
                    is GameState.MusicLibrary -> GameMusicLibraryLayout()
                    is GameState.Prepare -> GamePrepareLayout(state.info)
                    is GameState.Playing -> engine.ViewportContent(modifier = Modifier.fillMaxSize())
                    is GameState.Rank -> GameRankLayout()
                }
            }
        }
    }
}