package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import love.yinlin.foundation.Context

@Composable
expect fun VideoPlayer(controller: VideoController, modifier: Modifier = Modifier)

expect fun buildVideoController(
    context: Context,
    scope: CoroutineScope,
    audioFocus: Boolean,
    topBar: VideoActionBar? = null,
    bottomBar: VideoActionBar? = VideoActionBar.Progress
): VideoController