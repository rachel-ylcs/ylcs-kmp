@file:OptIn(UnstableApi::class)
@file:JvmName("MusicFactoryAndroid")
package love.yinlin.platform

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.*
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
import kotlinx.io.files.Path
import love.yinlin.R
import love.yinlin.common.FfmpegRenderersFactory
import love.yinlin.common.LocalFileProvider
import love.yinlin.data.Data
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.mutableRefStateOf
import love.yinlin.platform.MusicFactory.Companion.UPDATE_INTERVAL
import love.yinlin.service.CustomCommands
import love.yinlin.service.MusicService
import love.yinlin.ui.screen.music.audioPath
import love.yinlin.ui.screen.music.recordPath
import java.io.File

fun mergePlayMode(repeatMode: Int, shuffleModeEnabled: Boolean): MusicPlayMode = when {
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

class ActualMusicFactory(private val context: Context) : MusicFactory() {
    private var controller: MediaController? by mutableRefStateOf(null)
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

    private suspend fun send(command: SessionCommand, args: Bundle = Bundle.EMPTY): Data<Bundle> {
        val result = Coroutines.main { controller?.sendCustomCommand(command, args)?.get() }
        if (result == null) return Data.Failure()
        return if (result.resultCode == SessionResult.RESULT_SUCCESS) Data.Success(result.extras) else Data.Failure(message = "${result.sessionError}")
    }

    override var error: Throwable? by mutableRefStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override var musicList: List<MusicInfo> by mutableRefStateOf(emptyList())
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableRefStateOf(null)

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
            withPlayer {
                val newPlayMode = mergePlayMode(repeatMode, it.shuffleModeEnabled)
                playMode = newPlayMode
                onPlayModeChanged(playMode)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled)
            withPlayer {
                val newPlayMode = mergePlayMode(it.repeatMode, shuffleModeEnabled)
                playMode = newPlayMode
                onPlayModeChanged(playMode)
            }
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
                            if (!Coroutines.isActive()) break
                            currentPosition = player.currentPosition
                            delay(UPDATE_INTERVAL)
                        }
                    } else null
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            withPlayer {
                when (reason) {
                    Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED if timeline !is Timeline.RemotableTimeline -> {
                        // timeline可能是 Timeline.RemotableTimeline 或 PlaylistTimeline
                        musicList = timeline.extractMediaItems.mapNotNull { musicLibrary[it.mediaId] }
                    }
                    Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> { }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            withPlayer { player ->
                when (playbackState) {
                    Player.STATE_IDLE -> { }
                    Player.STATE_BUFFERING -> { }
                    Player.STATE_READY -> updateDuration(player)
                    Player.STATE_ENDED -> {
                        // 当因为删除当前媒体等原因停止了循环播放, 但仍然有剩余媒体则继续循环播放
                        if (player.mediaItemCount != 0 && playMode == MusicPlayMode.LOOP) player.play()
                        else {
                            // 已停止播放
                            musicList = emptyList()
                            isPlaying = false
                            currentPosition = 0L
                            currentDuration = 0L
                            currentPlaylist = null
                            currentMusic = null
                            onPlayerStop()
                        }
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            withPlayer { player ->
                if (mediaItem == null) {
                    onMusicChanged(null)
                    player.stop()
                }
                else {
                    val musicInfo = musicLibrary[mediaItem.mediaId]
                    currentMusic = musicInfo
                    onMusicChanged(musicInfo)
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

    private val MusicInfo.asMediaItem: MediaItem get() = MediaItem.Builder()
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
                .setArtworkUri(LocalFileProvider.uri(
                    context = context,
                    authority = context.getString(R.string.music_file_provider),
                    file = File(this.recordPath.toString())
                )).build()
        ).build()

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        send(
            command = CustomCommands.SetMode,
            args = Bundle().apply { putInt(CustomCommands.Args.SET_MODE_ARG_MODE, musicPlayMode.ordinal) }
        )
    }

    override suspend fun play() = withMainPlayer { it.play() }
    override suspend fun pause() = withMainPlayer { it.pause() }
    override suspend fun stop() = withMainPlayer { it.stop() }
    override suspend fun gotoPrevious() = withMainPlayer { it.seekToPreviousMediaItem() }
    override suspend fun gotoNext() = withMainPlayer { it.seekToNextMediaItem() }
    override suspend fun gotoIndex(index: Int) = withMainPlayer { player ->
        if (index != -1 && player.currentMediaItemIndex != index) {
            player.seekTo(index, 0L)
        }
    }
    override suspend fun seekTo(position: Long) = withMainPlayer { it.seekTo(position) }
    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?, playing: Boolean) = withMainPlayer { player ->
        player.setMediaItems(medias.map { it.asMediaItem }, startIndex ?: C.INDEX_UNSET, C.TIME_UNSET)
        if (playing) player.play()
        else player.prepare()
    }
    override suspend fun addMedias(medias: List<MusicInfo>) = withMainPlayer { player ->
        player.addMediaItems(medias.map { it.asMediaItem })
    }
    override suspend fun removeMedia(index: Int) = withMainPlayer { it.removeMediaItem(index)  }
}

@Stable
actual class MusicPlayer {
    private var player by mutableRefStateOf<ExoPlayer?>(null)
    private var updateProgressJob: Job? = null
    private val updateProgressJobLock = Any()

    actual val isInit: Boolean get() = player != null

    private var mIsPlaying by mutableStateOf(false)
    actual val isPlaying: Boolean get() = mIsPlaying

    private var mPosition by mutableLongStateOf(0L)
    actual var position: Long get() = mPosition
        set(value) {
            player?.let {
                it.seekTo(value)
                if (!it.isPlaying) it.play()
            }
        }

    private var mDuration by mutableLongStateOf(0L)
    actual val duration: Long get() = mDuration

    actual suspend fun init() {
        player = FfmpegRenderersFactory.build(appNative.context, false).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            shuffleModeEnabled = false
            addListener(object : Player.Listener {
                private fun updateInfo() {
                    val p = this@apply.currentPosition
                    val d = this@apply.duration
                    mPosition = if (p == C.TIME_UNSET) 0L else p
                    mDuration = if (d == C.TIME_UNSET) 0L else d
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    mIsPlaying = isPlaying
                    synchronized(updateProgressJobLock) {
                        updateProgressJob?.cancel()
                        updateProgressJob = if (isPlaying) Coroutines.startMain {
                            while (true) {
                                if (!Coroutines.isActive()) break
                                mPosition = this@apply.currentPosition
                                delay(UPDATE_INTERVAL)
                            }
                        } else null
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_IDLE, Player.STATE_BUFFERING -> {}
                        Player.STATE_READY -> updateInfo()
                        Player.STATE_ENDED -> this@apply.clearMediaItems()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    updateInfo()
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    this@apply.clearMediaItems()
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

    actual suspend fun play() {
        player?.let {
            if (!it.isPlaying) it.play()
        }
    }

    actual suspend fun pause() {
        player?.let {
            if (it.isPlaying) it.pause()
        }
    }

    actual suspend fun stop() {
        player?.clearMediaItems()
    }

    actual fun release() {
        player?.release()
    }
}