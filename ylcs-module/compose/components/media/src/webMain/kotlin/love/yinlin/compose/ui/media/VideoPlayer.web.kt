package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.foundation.PlatformContext

@Composable
actual fun VideoPlayer(controller: VideoController, modifier: Modifier) {
    controller.view.HostView(modifier = modifier)
}

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = WebVideoController(topBar, bottomBar)