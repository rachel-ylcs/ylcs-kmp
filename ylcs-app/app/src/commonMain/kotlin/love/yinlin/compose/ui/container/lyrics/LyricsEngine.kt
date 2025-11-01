package love.yinlin.compose.ui.container.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface LyricsEngine {
    @Composable
    fun Content(modifier: Modifier, position: Long)
}