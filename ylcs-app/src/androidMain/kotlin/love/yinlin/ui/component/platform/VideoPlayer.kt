package love.yinlin.ui.component.platform

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import love.yinlin.common.Colors
import love.yinlin.common.FfmpegRenderersFactory
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.rememberState

@SuppressLint("SourceLockedOrientationActivity")
@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    var player: Player? by rememberState { null }

    DisposableEffect(Unit) {
        player = FfmpegRenderersFactory.build(context, false).apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
        onDispose {
            player?.release()
            player = null
        }
    }

    LifecycleStartEffect(Unit) {
        player?.play()
        onStopOrDispose {
            player?.pause()
        }
    }

    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().background(Colors.Black).zIndex(1f))
        player?.let {
            var showControls by rememberState { true }
            val presentationState = rememberPresentationState(it)
            val scaledModifier = Modifier.resizeWithContentScale(ContentScale.Inside, presentationState.videoSizeDp)
            val isLandscape by rememberDerivedState {
                presentationState.videoSizeDp?.let { size -> size.width > size.height }
            }

            val activity = LocalActivity.current!!
            val oldOrientation = remember { activity.requestedOrientation }
            DisposableEffect(isLandscape) {
                isLandscape?.let { value ->
                    if (value && activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    else if (!value && activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                onDispose {
                    if (activity.requestedOrientation != oldOrientation) activity.requestedOrientation = oldOrientation
                }
            }

            PlayerSurface(
                player = it,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = scaledModifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showControls = !showControls }
                ).zIndex(2f)
            )
        }
    }
}