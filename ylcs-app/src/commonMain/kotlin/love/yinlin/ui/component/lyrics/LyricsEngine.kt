package love.yinlin.ui.component.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
interface LyricsEngine {
    @Composable fun content(
        modifier: Modifier = Modifier,
        onLyricsClick: (Long) -> Unit
    )
}