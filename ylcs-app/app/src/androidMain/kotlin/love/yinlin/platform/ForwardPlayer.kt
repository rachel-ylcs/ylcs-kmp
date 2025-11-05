package love.yinlin.platform

import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.service.CustomCommands

internal fun mergePlayMode(repeatMode: Int, shuffleModeEnabled: Boolean): MusicPlayMode = when {
    shuffleModeEnabled -> MusicPlayMode.RANDOM
    repeatMode == Player.REPEAT_MODE_ONE -> MusicPlayMode.LOOP
    else -> MusicPlayMode.ORDER
}

internal val Timeline.extractMediaItems: List<MediaItem> get() {
    val items = mutableListOf<MediaItem>()
    val window = Timeline.Window()
    for (index in 0 ..< windowCount) {
        getWindow(index, window)
        items += window.mediaItem
    }
    return items
}

@OptIn(UnstableApi::class)
@Stable
class ForwardPlayer(basePlayer: ExoPlayer) : ForwardingPlayer(basePlayer) {
    override fun getAvailableCommands(): Player.Commands = CustomCommands.NotificationPlayerCommands

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