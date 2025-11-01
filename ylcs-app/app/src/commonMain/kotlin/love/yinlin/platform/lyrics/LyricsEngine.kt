package love.yinlin.platform.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.io.files.Path

@Stable
interface LyricsEngine {
    val type: LyricsEngineType

    suspend fun load(rootPath: Path): Boolean
    fun reset()
    fun update(position: Long)

    @Composable
    fun Content(config: LyricsEngineConfig)

    companion object {
        private val Line = LineLyricsEngine()
        private val Rhyme = RhymeLyricsEngine()

        val Default: LyricsEngine = Line

        operator fun get(type: LyricsEngineType): LyricsEngine = when (type) {
            LyricsEngineType.Line -> Line
            LyricsEngineType.Rhyme -> Rhyme
        }
    }
}