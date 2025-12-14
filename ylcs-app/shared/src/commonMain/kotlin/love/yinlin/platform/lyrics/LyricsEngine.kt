package love.yinlin.platform.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import kotlinx.io.files.Path

@Stable
interface LyricsEngine {
    val type: LyricsEngineType

    suspend fun load(rootPath: Path): Boolean
    suspend fun reset()
    fun update(position: Long)

    @Composable
    fun LyricsCanvas(modifier: Modifier, config: LyricsEngineConfig, host: LyricsEngineHost)

    @Composable
    fun FloatingLyricsCanvas(config: LyricsEngineConfig, textStyle: TextStyle)

    companion object {
        private val Line by lazy { LineLyricsEngine() }
        private val Rhyme by lazy { RhymeLyricsEngine() }

        val Default: LyricsEngine by lazy { Line }

        operator fun get(type: LyricsEngineType): LyricsEngine = when (type) {
            LyricsEngineType.Line -> Line
            LyricsEngineType.Rhyme -> Rhyme
        }
    }
}