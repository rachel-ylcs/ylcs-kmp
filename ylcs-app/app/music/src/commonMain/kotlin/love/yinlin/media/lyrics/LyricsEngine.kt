package love.yinlin.media.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import love.yinlin.fs.File

@Stable
interface LyricsEngine {
    val interval: Long
    val type: LyricsEngineType

    suspend fun load(rootPath: File): Boolean
    fun reset()
    fun update(position: Long)

    @Composable
    fun LyricsCanvas(config: LyricsEngineConfig, host: LyricsEngineHost)

    @Composable
    fun FloatingLyricsCanvas(modifier: Modifier = Modifier, config: LyricsEngineConfig, textStyle: TextStyle)

    companion object {
        private val Line by lazy { LineLyricsEngine() }
        private val Rhyme by lazy { RhymeLyricsEngine() }

        val Default: LyricsEngine by lazy { Line }

        internal fun clone(type: LyricsEngineType): LyricsEngine = when (type) {
            LyricsEngineType.Line -> LineLyricsEngine()
            LyricsEngineType.Rhyme -> RhymeLyricsEngine()
        }

        operator fun get(type: LyricsEngineType): LyricsEngine = when (type) {
            LyricsEngineType.Line -> Line
            LyricsEngineType.Rhyme -> Rhyme
        }
    }
}