package love.yinlin.compose.game.plugin

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.drawer.LayerOrder
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.fs.File

@Stable
class MusicLibraryPlugin(
    engine: Engine,
    private val modPath: File,
    private val rawLibrary: Collection<MusicInfo>?
) : Plugin(engine) {
    @Stable
    class Factory(private val modPath: File, private val rawLibrary: Collection<MusicInfo>?) : PluginFactory {
        override fun build(engine: Engine): Plugin = MusicLibraryPlugin(engine, modPath, rawLibrary)
    }

    override val layerOrder: Int = LayerOrder.Invisible

    val library = mutableListOf<MusicInfo>()

    override suspend fun onInitialize(): Boolean {
        if (rawLibrary == null) return false
        Coroutines.io {
            rawLibrary.mapNotNullTo(library) { info ->
                if (info.path(modPath, ModResourceType.Rhyme).exists()) info else null
            }
        }
        return true
    }

    override fun onRelease() {
        library.clear()
    }
}