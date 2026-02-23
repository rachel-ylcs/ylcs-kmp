package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.foundation.Context

@Composable
actual fun VideoPlayer(controller: VideoController, modifier: Modifier) {
    UnsupportedPlatformComponent(modifier = modifier)
}

actual fun buildVideoController(
    context: Context,
    scope: CoroutineScope,
    audioFocus: Boolean,
    topBar: VideoActionBar?,
    bottomBar: VideoActionBar?
): VideoController = IOSVideoController(context, topBar, bottomBar)