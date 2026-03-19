package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.foundation.PlatformContextProvider

@Composable
expect fun VideoPlayer(controller: VideoController, modifier: Modifier = Modifier)

expect fun buildVideoController(
    context: PlatformContextProvider,
    topBar: VideoActionBar? = null,
    bottomBar: VideoActionBar? = VideoActionBar.Progress
): VideoController