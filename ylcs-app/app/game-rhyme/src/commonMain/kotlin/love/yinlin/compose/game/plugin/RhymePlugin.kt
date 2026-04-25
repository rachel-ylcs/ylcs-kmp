package love.yinlin.compose.game.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.data.RhymePlayResult
import love.yinlin.compose.game.layer.BackgroundLayer
import love.yinlin.compose.game.layer.InteractLayer
import love.yinlin.compose.game.layer.MapLayer
import love.yinlin.compose.game.layer.MomentLayer
import love.yinlin.compose.game.layer.UILayer
import love.yinlin.compose.game.ui.RhymeBlurSurface
import love.yinlin.compose.game.ui.RhymeCommonButton
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import love.yinlin.media.buildAudioPlayer
import kotlin.reflect.KClass

@Stable
class RhymePlugin(
    context: PlatformContext,
    private val blurState: BlurState,
    private val endListener: (RhymePlayResult?) -> Unit,
    engine: Engine,
) : UIPlugin(engine) {
    @Stable
    class Factory(
        private val context: PlatformContext,
        private val blurState: BlurState,
        private val endListener: (RhymePlayResult?) -> Unit,
    ) : PluginFactory {
        override fun build(engine: Engine): Plugin = RhymePlugin(context, blurState, endListener, engine)
    }

    override val dependencies: List<KClass<out Plugin>> = listOf(ScenePlugin::class, SoundPlugin::class)

    private lateinit var scene: ScenePlugin

    private val player = buildAudioPlayer(context) {

    }

    private var currentInfo: RhymePlayInfo? = null

    // 初始化游戏
    suspend fun setupGame(playInfo: RhymePlayInfo, audio: File) {
        currentInfo = playInfo
        player.load(audio, true)
        val backgroundLayer = BackgroundLayer()
        val interactLayer = InteractLayer()
        val momentLayer = MomentLayer(playInfo, player)
        val uiLayer = UILayer(playInfo, momentLayer)
        val mapLayer = MapLayer(scene.camera, playInfo, momentLayer, interactLayer, uiLayer)
        // 先更新交互结果再处理地图
        scene += listOf(backgroundLayer, momentLayer, interactLayer, mapLayer, uiLayer)
    }

    // 停止游戏
    fun stopGame() {
        scene.reset()
        endListener(null)
        player.stop()
        currentInfo = null
    }

    override suspend fun onInitialize(): Boolean {
        scene = engine.plugin()
        Coroutines.main { player.init() }
        return true
    }

    override fun onRelease() {
        stopGame()
        player.release()
    }

    @Composable
    private fun PauseContent(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            RhymeBlurSurface(
                modifier = Modifier.width(Theme.size.cell1),
                blurState = blurState,
                shape = Theme.shape.v3,
                border = Theme.border.v7,
                contentPadding = Theme.padding.value5
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9, Alignment.CenterVertically)
                ) {
                    SimpleClipText(text = "暂停中", style = Theme.typography.v5.bold, modifier = Modifier.padding(Theme.padding.v9))
                    RhymeCommonButton(icon = Icons.Clear, text = "退出", onClick = ::stopGame, modifier = Modifier.fillMaxWidth())
                    RhymeCommonButton(icon = Icons.PlayArrow, text = "继续", onClick = { engine.isRunning = true }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    override fun BoxScope.Content() {
        if (!engine.isRunning) {
            PauseContent(modifier = Modifier.fillMaxSize().background(Theme.color.scrim.copy(alpha = 0.6f)).silentClick { }.zIndex(2f))
        }

        LaunchedEffect(engine.isRunning) {
            if (engine.isRunning) player.play()
            else player.pause()
        }
    }
}