package love.yinlin.compose.ui.media

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import love.yinlin.foundation.PlatformContext

expect fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory = VideoActionBar.None,
    bottomBar: VideoActionBar.Factory = VideoActionBar.Progress
): VideoController

@Composable
fun VideoPlayer(controller: VideoController, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        controller.SurfaceContent(modifier = Modifier.fillMaxSize().zIndex(1f))
        controller.PlayerControls(modifier = Modifier.fillMaxSize().clipToBounds().zIndex(2f))
    }
}