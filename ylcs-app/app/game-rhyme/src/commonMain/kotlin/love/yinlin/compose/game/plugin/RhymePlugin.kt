package love.yinlin.compose.game.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.data.RhymePlayInfo
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
import love.yinlin.data.rachel.rhyme.RhymePlayResult
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
        stopGame(scene.requireEntity<UILayer>().submitResult())
    }

    private var isGameRunning: Boolean by mutableRefStateOf(false)

    // 初始化游戏
    suspend fun setupGame(playInfo: RhymePlayInfo, character: Character, audio: File) {
        if (!isGameRunning) {
            isGameRunning = true
            // 加载音频
            player.load(audio, true)
            // 加载画布
            val backgroundLayer = BackgroundLayer(character, playInfo.characterCV)
            val interactLayer = InteractLayer()
            val momentLayer = MomentLayer(character, playInfo, player)
            val uiLayer = UILayer(character, playInfo, momentLayer)
            val mapLayer = MapLayer(scene.camera, character, playInfo, momentLayer, backgroundLayer, interactLayer, uiLayer)
            // 先更新交互结果再处理地图
            scene += listOf(backgroundLayer, momentLayer, interactLayer, mapLayer, uiLayer)
        }
    }

    // 停止游戏
    fun stopGame(result: RhymePlayResult?) {
        if (isGameRunning) {
            endListener(result)
            scene.reset()
            player.stop()
            isGameRunning = false
        }
    }

    override suspend fun onInitialize(): Boolean {
        scene = engine.plugin()
        Coroutines.main { player.init() }
        return true
    }

    override fun onRelease() {
        stopGame(null)
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
                    RhymeCommonButton(icon = Icons.Clear, text = "退出", onClick = { stopGame(null) }, modifier = Modifier.fillMaxWidth())
                    RhymeCommonButton(icon = Icons.PlayArrow, text = "继续", onClick = { engine.isRunning = true }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    override fun BoxScope.Content() {
        if (!engine.isRunning && isGameRunning) {
            PauseContent(modifier = Modifier.fillMaxSize().background(Theme.color.scrim.copy(alpha = 0.6f)).silentClick { }.zIndex(2f))
        }

        LaunchedEffect(engine.isRunning) {
            if (isGameRunning) {
                if (engine.isRunning) player.play()
                else player.pause()
            }
        }
    }
}