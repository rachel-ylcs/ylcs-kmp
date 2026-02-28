package love.yinlin.media

import androidx.compose.runtime.Stable
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.io.files.Path
import love.yinlin.foundation.Context

@Stable
internal class AndroidAudioPlayer(context: Context, onEndListener: () -> Unit) : AudioPlayer(context, onEndListener) {
    private var player: ExoPlayer? = null

    private val listener: Player.Listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                player?.clearMediaItems()
                onEndListener()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            player?.clearMediaItems()
        }
    }

    override val isInit: Boolean get() = player != null

    override val isPlaying: Boolean get() = player?.isPlaying ?: false

    override val position: Long get() = player?.currentPosition.let { if (it == null || it == C.TIME_UNSET) 0L else it }

    override val duration: Long get() = player?.duration.let { if (it == null || it == C.TIME_UNSET) 0L else it }

    override suspend fun init() {
        player = FfmpegRenderersFactory.build(context.application, false).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            shuffleModeEnabled = false
            addListener(listener)
        }
    }

    override fun release() {
        player?.removeListener(listener)
        player?.release()
        player = null
    }

    override suspend fun load(path: Path) {
        player?.let {
            it.setMediaItem(MediaItem.fromUri(path.toString()))
            it.prepare()
            it.play()
        }
    }

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun stop() {
        player?.clearMediaItems()
    }

    override fun seekTo(position: Long) {
        player?.let {
            it.seekTo(position)
            if (!it.isPlaying) it.play()
        }
    }
}

actual fun buildAudioPlayer(context: Context, onEndListener: () -> Unit): AudioPlayer = AndroidAudioPlayer(context, onEndListener)