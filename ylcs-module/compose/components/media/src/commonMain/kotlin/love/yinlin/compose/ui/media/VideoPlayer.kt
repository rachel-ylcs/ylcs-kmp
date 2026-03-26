package love.yinlin.compose.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import love.yinlin.foundation.PlatformContext

expect fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory = VideoActionBar.None,
    bottomBar: VideoActionBar.Factory = VideoActionBar.Progress
): VideoController

@Composable
expect fun VideoSurface(controller: VideoController, modifier: Modifier = Modifier)

@Composable
fun VideoPlayer(controller: VideoController, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        VideoSurface(controller = controller, modifier = Modifier.fillMaxSize().zIndex(1f))
        controller.VideoPlayerControls(modifier = Modifier.fillMaxSize().zIndex(2f))
    }
}