package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.foundation.PlatformContextProvider

@Composable
actual fun VideoPlayer(controller: VideoController, modifier: Modifier) {
    UnsupportedPlatformComponent(modifier = modifier)
}

actual fun buildVideoController(
    context: PlatformContextProvider,
    topBar: VideoActionBar?,
    bottomBar: VideoActionBar?
): VideoController = IOSVideoController(context, topBar, bottomBar)