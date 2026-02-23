package love.yinlin.compose.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import love.yinlin.foundation.Context

@Composable
actual fun VideoPlayer(controller: VideoController, modifier: Modifier) {
    controller.view.HostView(modifier = modifier)
}

actual fun buildVideoController(
    context: Context,
    scope: CoroutineScope,
    audioFocus: Boolean,
    topBar: VideoActionBar?,
    bottomBar: VideoActionBar?
): VideoController = WebVideoController(context, topBar, bottomBar)