package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.foundation.PlatformContext

@Composable
expect fun VideoPlayer(controller: VideoController, modifier: Modifier = Modifier)

expect fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory = VideoActionBar.None,
    bottomBar: VideoActionBar.Factory = VideoActionBar.Progress
): VideoController