package love.yinlin.compose.ui.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
interface LyricsEngine {
    fun updateIndex(position: Long): String?

    @Composable fun Content(
        modifier: Modifier = Modifier,
        onLyricsClick: (Long) -> Unit
    )
}