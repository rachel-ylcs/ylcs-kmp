package love.yinlin.platform

import androidx.compose.runtime.Stable
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.common.FfmpegRenderersFactory

@Stable
actual class AudioPlayer actual constructor(private val context: Context) {
    private var player: ExoPlayer? = null

    actual val isInit: Boolean get() = player != null

    actual val isPlaying: Boolean get() = player?.isPlaying == true

    actual val position: Long get() = player?.currentPosition.let { if (it == null || it == C.TIME_UNSET) 0L else it }

    actual val duration: Long get() = player?.duration.let { if (it == null || it == C.TIME_UNSET) 0L else it }

    actual suspend fun init() {
        player = FfmpegRenderersFactory.build(context.application, false).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            shuffleModeEnabled = false
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_ENDED) innerStop()
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    innerStop()
                }
            })
        }
    }

    actual suspend fun load(path: Path) {
        player?.let {
            it.setMediaItem(MediaItem.fromUri(path.toString()))
            it.prepare()
            it.play()
        }
    }

    actual fun play() {
        player?.play()
    }

    actual fun pause() {
        player?.pause()
    }

    private fun innerStop() {
        player?.clearMediaItems()
    }

    actual fun stop() {
        innerStop()
    }

    actual fun release() {
        player?.release()
    }
}