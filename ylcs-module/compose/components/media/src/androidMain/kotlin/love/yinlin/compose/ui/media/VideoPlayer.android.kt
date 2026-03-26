package love.yinlin.compose.ui.media

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import love.yinlin.foundation.PlatformContext

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = AndroidVideoController(context, topBar, bottomBar)

@OptIn(UnstableApi::class)
@Composable
actual fun VideoSurface(controller: VideoController, modifier: Modifier) {
    val presentationState = rememberPresentationState(controller.exoPlayer)

    PlayerSurface(
        player = controller.exoPlayer,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = modifier.resizeWithContentScale(ContentScale.Inside, presentationState.videoSizeDp)
    )
}