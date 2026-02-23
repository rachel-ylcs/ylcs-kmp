package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import love.yinlin.media.FfmpegRenderersFactory
import love.yinlin.foundation.Context

@Stable
actual abstract class VideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoState(context, topBar, bottomBar) {
    internal abstract val exoPlayer: ExoPlayer

    actual override fun release() { }
}

internal class AndroidVideoController(context: Context, scope: CoroutineScope, audioFocus: Boolean, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoController(context, topBar, bottomBar) {
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            isPlayingFlow.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) this@AndroidVideoController.duration = exoPlayer.duration
        }
    }

    override val exoPlayer: ExoPlayer = FfmpegRenderersFactory.build(context.activity, audioFocus).apply {
        repeatMode = Player.REPEAT_MODE_ONE
        addListener(listener)
    }

    private val isPlayingFlow = MutableStateFlow(false)

    init {
        scope.launch {
            isPlayingFlow.collectLatest { value ->
                isPlaying = value
                if (value) {
                    while (true) {
                        position = exoPlayer.currentPosition
                        delay(100L)
                    }
                }
            }
        }
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

    override fun release() {
        exoPlayer.removeListener(listener)
        exoPlayer.release()
        orientationController.restore()
    }
}