package love.yinlin.compose.ui.media

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.PresentationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.coroutines.mainContext
import love.yinlin.foundation.PlatformContext
import love.yinlin.media.FfmpegRenderersFactory
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
private class AndroidVideoController(context: PlatformContext, topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    val scope = CoroutineScope(SupervisorJob() + mainContext)

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            isPlayingFlow.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) this@AndroidVideoController.duration = exoPlayer.duration
        }
    }

    val exoPlayer: ExoPlayer = FfmpegRenderersFactory.build(context, true).apply {
        repeatMode = Player.REPEAT_MODE_ONE
        addListener(listener)
    }

    val presentationState = PresentationState(keepContentOnReset = false)

    val isPlayingFlow = MutableStateFlow(false)

    init {
        scope.launch {
            isPlayingFlow.collectLatest { value ->
                isPlaying = value
                if (value) {
                    while (isActive) {
                        position = exoPlayer.currentPosition
                        delay(100.milliseconds)
                    }
                }
            }
        }
    }

    override fun releaseController() {
        scope.cancel()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }

    override fun load(path: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(path))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun play() {
        if (!exoPlayer.isPlaying) exoPlayer.play()
    }

    override fun pause() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
    }

    override fun stop() {
        exoPlayer.stop()
    }

    override fun seek(position: Long) {
        exoPlayer.seekTo(position)
        exoPlayer.play()
    }

    @Composable
    override fun SurfaceContent(modifier: Modifier) {
        LaunchedEffect(Unit) {
            presentationState.observe(exoPlayer)
        }

        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = modifier.resizeWithContentScale(ContentScale.Inside, presentationState.videoSizeDp)
        )
    }
}

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = AndroidVideoController(context, topBar, bottomBar)