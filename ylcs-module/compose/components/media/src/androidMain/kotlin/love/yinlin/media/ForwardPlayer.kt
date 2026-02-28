package love.yinlin.media

import androidx.compose.runtime.Stable
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import love.yinlin.compose.data.media.MediaPlayMode

fun mergePlayMode(repeatMode: Int, shuffleModeEnabled: Boolean): MediaPlayMode = when {
    shuffleModeEnabled -> MediaPlayMode.Random
    repeatMode == Player.REPEAT_MODE_ONE -> MediaPlayMode.Loop
    else -> MediaPlayMode.Order
}

@Stable
class ForwardPlayer(basePlayer: ExoPlayer) : ForwardingPlayer(basePlayer) {
    override fun getAvailableCommands(): Player.Commands = MediaCommands.NotificationPlayerCommands

    override fun seekToPrevious() = seekToPreviousMediaItem()
    override fun seekToNext() = seekToNextMediaItem()

    override fun seekToPreviousMediaItem() {
        super.seekToPreviousMediaItem()
        if (!isPlaying) play()
    }

    override fun seekToNextMediaItem() {
        super.seekToNextMediaItem()
        if (!isPlaying) play()
    }

    override fun seekTo(positionMs: Long) {
        super.seekTo(positionMs)
        if (!isPlaying) play()
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        super.seekTo(mediaItemIndex, positionMs)
        if (!isPlaying) play()
    }

    override fun play() {
        if (!isPlaying) super.play()
    }

    override fun pause() {
        if (isPlaying) super.pause()
    }

    override fun stop() {
        super.clearMediaItems()
    }
}