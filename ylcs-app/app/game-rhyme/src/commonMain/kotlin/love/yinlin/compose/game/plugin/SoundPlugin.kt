package love.yinlin.compose.game.plugin

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine
import love.yinlin.fs.File
import love.yinlin.media.SoundPlayer
import kotlin.reflect.KClass

@Stable
class SoundPlugin(engine: Engine, private val soundList: List<File>) : Plugin(engine) {
    @Stable
    class Factory(private val soundList: List<File>) : PluginFactory {
        override fun build(engine: Engine): Plugin = SoundPlugin(engine, soundList)
    }

    override val dependencies: List<KClass<out Plugin>> = listOf(ScenePlugin::class)

    private val player = SoundPlayer()

    override suspend fun onInitialize(): Boolean {
        player.loadFromPath(soundList)
        return true
    }

    override fun onRelease() {
        player.release()
    }
}