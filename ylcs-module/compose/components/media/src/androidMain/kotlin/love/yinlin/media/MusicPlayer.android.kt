package love.yinlin.media

import android.content.ComponentName
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.coroutines.Coroutines
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.extension.replaceAll
import love.yinlin.foundation.Context
import love.yinlin.uri.Uri
import love.yinlin.uri.toAndroidUri

@OptIn(UnstableApi::class)
@Stable
class AndroidMusicPlayer<Info : MediaInfo>(fetcher: MediaMetadataFetcher<Info>) : MusicPlayer<Info>(fetcher) {
    private var controller: MediaController? by mutableRefStateOf(null)

    private val scope = CoroutineScope(SupervisorJob() + mainContext)

    private val isPlayingFlow = MutableStateFlow(false)

    private val androidListener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) = withPlayer {
            val newPlayMode = mergePlayMode(repeatMode, it.shuffleModeEnabled)
            playMode = newPlayMode
            listener?.onPlayModeChanged(newPlayMode)
        } ?: Unit

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = withPlayer {
            val newPlayMode = mergePlayMode(it.repeatMode, shuffleModeEnabled)
            playMode = newPlayMode
            listener?.onPlayModeChanged(newPlayMode)
        } ?: Unit

        override fun onIsPlayingChanged(value: Boolean) = withPlayer {
            isPlayingFlow.value = value
        } ?: Unit

        override fun onPlaybackStateChanged(playbackState: Int) = withPlayer {
            when (playbackState) {
                Player.STATE_IDLE -> { }
                Player.STATE_BUFFERING -> { }
                Player.STATE_READY -> updateDuration(it)
                Player.STATE_ENDED -> {
                    // 当因为删除当前媒体等原因停止了循环播放, 但仍然有剩余媒体则继续循环播放
                    if (it.mediaItemCount != 0 && playMode == MediaPlayMode.Loop) it.play()
                    else {
                        // 已停止播放
                        musicList.clear()
                        isPlayingFlow.value = false
                        position = 0L
                        duration = 0L
                        music = null
                        listener?.onPlayerStop()
                    }
                }
            }
        } ?: Unit

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = withPlayer {
            if (mediaItem == null) {
                listener?.onMusicChanged(null)
                it.stop()
            }
            else {
                val musicInfo = musicList.find { item -> item.id == mediaItem.mediaId }
                music = musicInfo
                listener?.onMusicChanged(musicInfo)
                updateDuration(it)
            }
        } ?: Unit

        override fun onPlayerError(err: PlaybackException) = withPlayer {
            it.stop()
            error = err
        } ?: Unit
    }

    override suspend fun init(context: Context) {
        val ctx = context.application
        val (pkg, cls) = fetcher.androidMusicServiceComponentName

        scope.launch {
            isPlayingFlow.collectLatest { value ->
                isPlaying = value
                if (value) {
                    while (isActive) {
                        position = controller?.currentPosition ?: 0L
                        delay(fetcher.interval)
                    }
                }
            }
        }

        catchingNull {
            val mediaController: MediaController? = Coroutines.sync { future ->
                val token = SessionToken(ctx, ComponentName(pkg, cls))
                val bitmapLoader = DataSourceBitmapLoader.Builder(ctx).setMakeShared(true).build()
                val callback = MediaController.Builder(ctx, token).setBitmapLoader(bitmapLoader).buildAsync()
                callback.addListener({
                    future.send { callback.get() }
                }, MoreExecutors.directExecutor())
            }
            if (mediaController != null) {
                mediaController.removeListener(androidListener)
                mediaController.addListener(androidListener)
                controller = mediaController
            }
            isInit = controller != null
        }
    }

    override fun release() {
        scope.cancel()
        controller?.removeListener(androidListener)
        controller?.release()
        controller = null
    }

    private inline fun <R> withPlayer(block: (MediaController) -> R): R? = controller?.let { player ->
        catchingDefault({
            error = it
            null
        }) {
            block(player)
        }
    }

    private suspend inline fun <R> withMainPlayer(crossinline block: (MediaController) -> R): R? = controller?.let { player ->
        catchingDefault({
            error = it
            null
        }) {
            Coroutines.main { block(player) }
        }
    }

    private val Info.asMediaItem: MediaItem get() = MediaItem.Builder()
        .setMediaId(this.id)
        .setUri(with(fetcher) { audioUri })
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.name)
                .setArtist(this.singer)
                .setAlbumTitle(this.album)
                .setAlbumArtist(this.singer)
                .setComposer(this.composer)
                .setWriter(this.lyricist)
                .setArtworkUri(Uri.parse(with(fetcher) { coverUri })?.toAndroidUri() ?: android.net.Uri.EMPTY)
                .build()
        ).build()

    private fun updateDuration(player: Player) {
        position = player.currentPosition.let { if (it == C.TIME_UNSET) 0L else it }
        duration = player.duration.let { if (it == C.TIME_UNSET) 0L else it }
    }

    private suspend fun send(command: SessionCommand, args: Bundle = Bundle.EMPTY): Bundle? = withMainPlayer {
        it.sendCustomCommand(command, args).get()
    }?.let {
        if (it.resultCode == SessionResult.RESULT_SUCCESS) it.extras else null
    }

    override suspend fun updatePlayMode(mode: MediaPlayMode) {
        send(
            command = MediaCommands.SetMode,
            args = Bundle().apply { putInt(MediaCommands.Args.SET_MODE_ARG_MODE, mode.ordinal) }
        )
    }

    override suspend fun play() = withMainPlayer { it.play() } ?: Unit

    override suspend fun pause() = withMainPlayer { it.pause() } ?: Unit

    override suspend fun stop() = withMainPlayer { it.stop() } ?: Unit

    override suspend fun gotoPrevious() = withMainPlayer { it.seekToPreviousMediaItem() } ?: Unit

    override suspend fun gotoNext() = withMainPlayer { it.seekToNextMediaItem() } ?: Unit

    override suspend fun gotoIndex(index: Int) = withMainPlayer {
        if (index != -1 && it.currentMediaItemIndex != index) it.seekTo(index, 0L)
    } ?: Unit

    override suspend fun seekTo(position: Long) = withMainPlayer { it.seekTo(position) } ?: Unit

    override suspend fun prepareMedias(medias: List<Info>, startIndex: Int?, playing: Boolean) = withMainPlayer {
        musicList.replaceAll(medias)
        it.setMediaItems(medias.map { item -> item.asMediaItem }, startIndex ?: C.INDEX_UNSET, C.TIME_UNSET)
        if (playing) it.play()
        else it.prepare()
    } ?: Unit

    override suspend fun addMedias(medias: List<Info>) = withMainPlayer {
        it.addMediaItems(medias.map { item -> item.asMediaItem })
    } ?: Unit

    override suspend fun removeMedia(index: Int) = withMainPlayer {
        it.removeMediaItem(index)
    } ?: Unit
}

actual fun <Info : MediaInfo> buildMusicPlayer(fetcher: MediaMetadataFetcher<Info>): MusicPlayer<Info> = AndroidMusicPlayer(fetcher)