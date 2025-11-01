package love.yinlin.platform.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.io.files.Path

@Stable
internal class RhymeLyricsEngine : LyricsEngine {
    override val type: LyricsEngineType = LyricsEngineType.Rhyme

    override suspend fun load(rootPath: Path): Boolean {
        return false
    }

    override fun reset() {

    }

    override fun update(position: Long) {

    }

    @Composable
    override fun Content(config: LyricsEngineConfig) {

    }
}