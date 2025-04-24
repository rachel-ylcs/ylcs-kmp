package love.yinlin.ui.component.platform

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import love.yinlin.ui.component.image.MiniIcon

@SuppressLint("SourceLockedOrientationActivity")
@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    var controller: Player? by rememberState { null }

    DisposableEffect(Unit) {
        controller = FfmpegRenderersFactory.build(context, false).apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
        onDispose {
            controller?.release()
            controller = null
        }
    }

    LifecycleStartEffect(Unit) {
        controller?.play()
        onStopOrDispose {
            controller?.pause()
        }
    }

    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().background(Colors.Black).zIndex(1f))
        controller?.let { player ->
            val presentationState = rememberPresentationState(player)
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
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = scaledModifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (player.isPlaying) player.pause()
                        else player.play()
                    }
                ).zIndex(2f)
            )

            if (!player.isPlaying) {
                MiniIcon(
                    icon = Icons.Outlined.SmartDisplay,
                    color = Colors.White,
                    size = 48.dp,
                    modifier = Modifier.align(Alignment.Center).zIndex(3f),
                )
            }
        }
    }
}