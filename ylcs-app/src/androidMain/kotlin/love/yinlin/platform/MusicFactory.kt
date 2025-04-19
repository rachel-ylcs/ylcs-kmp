@file:OptIn(UnstableApi::class)
@file:JvmName("MusicFactoryAndroid")
package love.yinlin.platform

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import love.yinlin.R
import love.yinlin.data.Data
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.service.CustomCommands
import love.yinlin.service.MusicService
import love.yinlin.ui.screen.music.audioPath
import love.yinlin.ui.screen.music.recordPath
import java.io.File

private fun mergePlayMode(repeatMode: Int, shuffleModeEnabled: Boolean): MusicPlayMode = when {
    shuffleModeEnabled -> MusicPlayMode.RANDOM
    repeatMode == Player.REPEAT_MODE_ONE -> MusicPlayMode.LOOP
    else -> MusicPlayMode.ORDER
}

private val Timeline.extractMediaItems: List<MediaItem> get() {
    val items = mutableListOf<MediaItem>()
    val window = Timeline.Window()
    for (index in 0 ..< windowCount) {
        getWindow(index, window)
        items += window.mediaItem
    }
    return items
}

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

    override fun play() {
        if (!isPlaying) super.play()
    }

    override fun pause() {
        if (isPlaying) super.pause()
    }

    override fun stop() {
        clearMediaItems()
        super.stop()
    }
}

class ActualMusicFactory(private val context: Context) : MusicFactory() {
    var controller: MediaController? by mutableStateOf(null)
    override val isInit: Boolean by derivedStateOf { controller != null }

    override suspend fun init() {
        Coroutines.main {
            val mediaController = Coroutines.io {
                val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
                MediaController.Builder(context, sessionToken).buildAsync().get()
            }
            mediaController.removeListener(listener)
            mediaController.addListener(listener)
            controller = mediaController
            updatePlayMode(MusicPlayMode.ORDER)
        }
    }

    private suspend inline fun withMainPlayer(crossinline block: CoroutineScope.(player: MediaController) -> Unit) {
        controller?.let {
            Coroutines.main { block(it) }
        }
    }

    private inline fun withPlayer(block: (player: MediaController) -> Unit) {
        controller?.let(block)
    }

    private fun send(command: SessionCommand, args: Bundle = Bundle.EMPTY): Data<Bundle> {
        val result = controller?.sendCustomCommand(command, args)?.get()
        if (result == null) return Data.Error()
        return if (result.resultCode == SessionResult.RESULT_SUCCESS) Data.Success(result.extras) else Data.Error(message = "${result.sessionError}")
    }

    override var error: Throwable? by mutableStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override var musicList: List<MusicInfo> by mutableStateOf(emptyList())
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentDuration: Long by mutableLongStateOf(0L)

    private var updateProgressJob: Job? = null
    private val updateProgressJobLock = Any()

    private fun updateDuration(player: Player) {
        val position = player.currentPosition
        val duration = player.duration
        currentPosition = if (position == C.TIME_UNSET) 0L else position
        currentDuration = if (duration == C.TIME_UNSET) 0L else duration
    }

    val listener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            withPlayer { playMode = mergePlayMode(repeatMode, it.shuffleModeEnabled) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled)
            withPlayer { playMode = mergePlayMode(it.repeatMode, shuffleModeEnabled) }
        }

        override fun onIsPlayingChanged(value: Boolean) {
            super.onIsPlayingChanged(value)
            withPlayer { player ->
                isPlaying = value
                // 更新进度
                synchronized(updateProgressJobLock) {
                    updateProgressJob?.cancel()
                    updateProgressJob = if (value) Coroutines.startMain {
                        while (true) {
                            if (!isActive) break
                            currentPosition = player.currentPosition
                            delay(UPDATE_INTERVAL)
                        }
                    } else null
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            withPlayer { player ->
                when (reason) {
                    Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED -> {
                        // timeline可能是 Timeline.RemotableTimeline 或 PlaylistTimeline
                        if (timeline !is Timeline.RemotableTimeline) {
                            musicList = timeline.extractMediaItems.mapNotNull { musicLibrary[it.mediaId] }
                        }
                    }
                    Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> { }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            withPlayer { player ->
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        // 已停止播放
                        currentPosition = 0L
                        currentDuration = 0L
                        currentMusic = null
                        currentPlaylist = null
                        musicList = emptyList()
                    }
                    Player.STATE_BUFFERING -> { }
                    Player.STATE_READY -> updateDuration(player)
                    Player.STATE_ENDED -> {
                        // 当因为删除当前媒体等原因停止了循环播放, 但仍然有剩余媒体则继续循环播放
                        if (player.mediaItemCount != 0 && playMode == MusicPlayMode.LOOP) player.play()
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            withPlayer { player ->
                if (mediaItem == null) player.stop()
                else {
                    currentMusic = musicLibrary[mediaItem.mediaId]
                    updateDuration(player)
                }
            }
        }

        override fun onPlayerError(err: PlaybackException) {
            super.onPlayerError(err)
            withPlayer { player ->
                player.stop()
                error = err
            }
        }
    }

    private val MusicInfo.asMediaItem get(): MediaItem = MediaItem.Builder()
        .setMediaId(this.id)
        .setUri(this.audioPath.toString().toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.name)
                .setArtist(this.singer)
                .setAlbumTitle(this.album)
                .setAlbumArtist(this.singer)
                .setComposer(this.composer)
                .setWriter(this.lyricist)
                .setArtworkUri(FileProvider.getUriForFile(
                    context,
                    context.getString(R.string.music_file_provider),
                        File(this.recordPath.toString())
                ))
                .build()
        )
        .build()

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) = withMainPlayer { player ->
        when (musicPlayMode) {
            MusicPlayMode.ORDER -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            MusicPlayMode.LOOP -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            MusicPlayMode.RANDOM -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = true
            }
        }
    }

    override suspend fun play() = withMainPlayer { it.play() }
    override suspend fun pause() = withMainPlayer { it.pause() }
    override suspend fun stop() = withMainPlayer { it.stop() }
    override suspend fun gotoPrevious() = withMainPlayer { it.seekToPreviousMediaItem() }
    override suspend fun gotoNext() = withMainPlayer { it.seekToNextMediaItem() }
    override suspend fun gotoIndex(index: Int) = withMainPlayer { player ->
        if (index != -1 && player.currentMediaItemIndex != index) {
            player.seekTo(index, 0L)
            if (!player.isPlaying) player.play()
        }
    }
    override suspend fun seekTo(position: Long) = withMainPlayer { it.seekTo(position) }
    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?) = withMainPlayer { player ->
        player.setMediaItems(medias.map { it.asMediaItem }, startIndex ?: C.INDEX_UNSET, C.TIME_UNSET)
    }
    override suspend fun addMedia(media: MusicInfo) = withMainPlayer { it.addMediaItem(media.asMediaItem) }
    override suspend fun addMedias(medias: List<MusicInfo>) = withMainPlayer { player ->
        player.addMediaItems(medias.map { it.asMediaItem })
    }
    override suspend fun removeMedia(index: Int) = withMainPlayer { it.removeMediaItem(index)  }
    override suspend fun moveMedia(start: Int, end: Int) = withMainPlayer { it.moveMediaItem(start, end) }
}