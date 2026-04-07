package love.yinlin.compose.game.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.data.RhymePlayResult
import love.yinlin.compose.game.ui.RhymeAcrylicSurface
import love.yinlin.compose.game.ui.RhymeCommonButton
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import love.yinlin.media.buildAudioPlayer
import kotlin.reflect.KClass

@Stable
class RhymePlugin(
    engine: Engine,
    context: PlatformContext,
    private val endListener: (RhymePlayResult?) -> Unit
) : UIPlugin(engine) {
    @Stable
    class Factory(
        private val context: PlatformContext,
        private val endListener: (RhymePlayResult?) -> Unit
    ) : PluginFactory {
        override fun build(engine: Engine): Plugin = RhymePlugin(engine, context, endListener)
    }

    override val dependencies: List<KClass<out Plugin>> = listOf(ScenePlugin::class, SoundPlugin::class)

    private lateinit var scene: ScenePlugin

    private val player = buildAudioPlayer(context) {

    }

    private var info: RhymePlayInfo? = null

    suspend fun setupGame(playInfo: RhymePlayInfo, audio: File) {
        info = playInfo
        player.load(audio, true)
    }

    fun stopGame() {
        player.stop()
        info = null
        endListener(null)
    }

    fun resetGame() {

    }

    override suspend fun onInitialize(): Boolean {
        scene = engine.plugin()
        Coroutines.main { player.init() }
        return true
    }

    override fun onRelease() {
        player.release()
    }

    @Composable
    override fun BoxScope.Content() {
        LaunchedEffect(engine.isRunning) {
            if (engine.isRunning) player.play()
            else player.pause()
        }

        if (engine.isRunning) {
            PrimaryButton("暂停", onClick = {
                engine.isRunning = false
            })
        }
        else {
            Box(
                modifier = Modifier.fillMaxSize().background(Theme.color.scrim.copy(alpha = 0.5f)).silentClick { },
                contentAlignment = Alignment.Center
            ) {
                RhymeAcrylicSurface(
                    modifier = Modifier.width(Theme.size.cell1),
                    shape = Theme.shape.v3,
                    contentPadding = Theme.padding.value5
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9, Alignment.CenterVertically)
                    ) {
                        SimpleClipText(text = "暂停中", style = Theme.typography.v5.bold)
                        RhymeCommonButton(icon = Icons.Clear, text = "退出", onClick = ::stopGame, modifier = Modifier.fillMaxWidth())
                        RhymeCommonButton(icon = Icons.PlayArrow, text = "继续", onClick = { engine.isRunning = true }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}