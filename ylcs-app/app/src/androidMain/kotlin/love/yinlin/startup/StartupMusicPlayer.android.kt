package love.yinlin.startup

import android.content.ComponentName
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import love.yinlin.AndroidContext
import love.yinlin.Context
import love.yinlin.R
import love.yinlin.StartupFetcher
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.fixup.Fixup
import love.yinlin.platform.Coroutines
import love.yinlin.platform.extractMediaItems
import love.yinlin.platform.mergePlayMode
import love.yinlin.service.CustomCommands
import love.yinlin.service.MusicService
import java.io.File

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class, nullable = true)
@Stable
actual fun buildMusicPlayer(): StartupMusicPlayer = object : StartupMusicPlayer() {
    private var controller: MediaController? by mutableRefStateOf(null)
    private lateinit var androidContext: AndroidContext

    override val isInit: Boolean by derivedStateOf { controller != null }
    override var error: Throwable? by mutableRefStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override var musicList: List<MusicInfo> by mutableRefStateOf(emptyList())
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableRefStateOf(null)

    private var updateProgressJob: Job? = null
    private val updateProgressJobLock = SynchronizedObject()

    override suspend fun initController(context: Context) {
        Coroutines.main {
            androidContext = context.application
            val mediaController = Coroutines.io {
                val sessionToken = SessionToken(androidContext, ComponentName(androidContext, MusicService::class.java))
                MediaController.Builder(androidContext, sessionToken).buildAsync().get()
            }
            mediaController.removeListener(listener)
            mediaController.addListener(listener)
            controller = mediaController
        }
    }

    private suspend inline fun withMainPlayer(crossinline block: (player: MediaController) -> Unit) {
        controller?.let {
            Coroutines.main { block(it) }
        }
    }

    private inline fun withPlayer(block: (player: MediaController) -> Unit) {
        controller?.let(block)
    }

    @OptIn(UnstableApi::class)
    private suspend fun send(command: SessionCommand, args: Bundle = Bundle.EMPTY): Bundle? = Coroutines.main {
        controller?.sendCustomCommand(command, args)?.get()
    }?.let {
        if (it.resultCode == SessionResult.RESULT_SUCCESS) it.extras else null
    }

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

    val listener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) {
            withPlayer {
                val newPlayMode = mergePlayMode(repeatMode, it.shuffleModeEnabled)
                playMode = newPlayMode
                onPlayModeChanged(playMode)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            withPlayer {
                val newPlayMode = mergePlayMode(it.repeatMode, shuffleModeEnabled)
                playMode = newPlayMode
                onPlayModeChanged(playMode)
            }
        }

        override fun onIsPlayingChanged(value: Boolean) {
            withPlayer { player ->
                isPlaying = value
                // 更新进度
                synchronized(updateProgressJobLock) {
                    updateProgressJob?.cancel()
                    updateProgressJob = if (value) Coroutines.startMain {
                        while (true) {
                            if (!Coroutines.isActive()) break
                            currentPosition = player.currentPosition
                            delay(PROGRESS_UPDATE_INTERVAL)
                        }
                    } else null
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            withPlayer {
                when (reason) {
                    Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED if timeline !is Timeline.RemotableTimeline -> {
                        // timeline可能是 Timeline.RemotableTimeline 或 PlaylistTimeline
                        musicList = timeline.extractMediaItems.mapNotNull { library[it.mediaId] }
                    }
                    Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> { }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
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
                            playlist = null
                            currentMusic = null
                            onPlayerStop()
                        }
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            withPlayer { player ->
                if (mediaItem == null) {
                    onMusicChanged(null)
                    player.stop()
                }
                else {
                    val musicInfo = library[mediaItem.mediaId]
                    currentMusic = musicInfo
                    onMusicChanged(musicInfo)
                    updateDuration(player)
                }
            }
        }

        override fun onPlayerError(err: PlaybackException) {
            withPlayer { player ->
                player.stop()
                error = err
            }
        }
    }

    private val MusicInfo.asMediaItem: MediaItem get() = MediaItem.Builder()
        .setMediaId(this.id)
        .setUri(this.path(ModResourceType.Audio).toString().toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.name)
                .setArtist(this.singer)
                .setAlbumTitle(this.album)
                .setAlbumArtist(this.singer)
                .setComposer(this.composer)
                .setWriter(this.lyricist)
                .setArtworkUri(Fixup.updateLocalFileProviderPermission(
                    context = androidContext,
                    authority = androidContext.getString(R.string.mod_file_provider),
                    file = File(this.path(ModResourceType.Record).toString())
                )).build()
        ).build()

    private fun updateDuration(player: Player) {
        val position = player.currentPosition
        val duration = player.duration
        currentPosition = if (position == C.TIME_UNSET) 0L else position
        currentDuration = if (duration == C.TIME_UNSET) 0L else duration
    }
}