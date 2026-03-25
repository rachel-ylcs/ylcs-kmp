package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

@Stable
actual abstract class VideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoState(topBar, bottomBar) {
    internal abstract val exoPlayer: ExoPlayer

    actual override fun releaseController() { }
}

internal class AndroidVideoController(context: PlatformContext, topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    private val scope = CoroutineScope(SupervisorJob() + mainContext)

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            isPlayingFlow.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) this@AndroidVideoController.duration = exoPlayer.duration
        }
    }

    override val exoPlayer: ExoPlayer = FfmpegRenderersFactory.build(context, true).apply {
        repeatMode = Player.REPEAT_MODE_ONE
        addListener(listener)
    }

    private val isPlayingFlow = MutableStateFlow(false)

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
}