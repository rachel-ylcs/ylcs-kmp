package love.yinlin.compose.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.Colors
import love.yinlin.foundation.Context

@Composable
actual fun VideoPlayer(controller: VideoController, modifier: Modifier) {
    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().background(Colors.Black).zIndex(1f))

        val presentationState = rememberPresentationState(controller.exoPlayer)
        val scaledModifier = Modifier.resizeWithContentScale(ContentScale.Inside, presentationState.videoSizeDp)

        PlayerSurface(
            player = controller.exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = scaledModifier.zIndex(2f)
        )

        controller.VideoPlayerControls(modifier = Modifier.matchParentSize().zIndex(3f))
    }
}

actual fun buildVideoController(
    context: Context,
    scope: CoroutineScope,
    audioFocus: Boolean,
    topBar: VideoActionBar?,
    bottomBar: VideoActionBar?
): VideoController = AndroidVideoController(context, scope, audioFocus, topBar, bottomBar)