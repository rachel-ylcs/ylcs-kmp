@file:OptIn(UnstableApi::class)
package love.yinlin.platform

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.io.files.Path
import love.yinlin.R
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.music.MusicResourceType
import love.yinlin.service.CustomCommands
import love.yinlin.service.MusicService
import java.io.File

private val MusicInfo.audioPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Audio.defaultFilename)
private val MusicInfo.recordPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Record.defaultFilename)

class ForwardPlayer(player: ExoPlayer, mode: MusicPlayMode) : ForwardingPlayer(player) {
    init {
        updatePlayMode(mode)
    }

    override fun getAvailableCommands(): Player.Commands = CustomCommands.NotificationPlayerCommands

    override fun seekToPrevious() = super.seekToPreviousMediaItem()
    override fun seekToNext() = super.seekToNextMediaItem()

    override fun seekToPreviousMediaItem() {
        super.seekToPreviousMediaItem()
        println("seekToPreviousMediaItem")
    }

    override fun seekToNextMediaItem() {
        super.seekToNextMediaItem()
        println("seekToNextMediaItem")
    }

    fun updatePlayMode(mode: MusicPlayMode) {
        when (mode) {
            MusicPlayMode.ORDER -> {
                repeatMode = REPEAT_MODE_ALL
                shuffleModeEnabled = false
            }
            MusicPlayMode.LOOP -> {
                repeatMode = REPEAT_MODE_ONE
                shuffleModeEnabled = false
            }
            MusicPlayMode.RANDOM -> {
                repeatMode = REPEAT_MODE_ALL
                shuffleModeEnabled = true
            }
        }
    }
}

class ActualMusicFactory(private val context: Context) : MusicFactory() {
    var controller: MediaController? by mutableStateOf(null)
    override val isInit: Boolean by derivedStateOf { controller != null }

    override suspend fun init() {
        Coroutines.main {
            initLibrary()
            val mediaController = Coroutines.io {
                val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
                MediaController.Builder(context, sessionToken).buildAsync().get()
            }
            mediaController.removeListener(listener)
            mediaController.addListener(listener)
            controller = mediaController
        }
    }

    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
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
                .setArtworkUri(FileProvider.getUriForFile(context, context.getString(R.string.app_file_provider), File(this.recordPath.toString())))
                .build()
        )
        .build()

    override suspend fun start(playlist: MusicPlaylist, startId: String?) {
        controller?.let { player ->
            if (currentPlaylist != playlist) {
                val mediaItems = mutableListOf<MediaItem>()
                for (id in playlist.items) musicLibrary[id]?.let { mediaItems += it.asMediaItem }
                if (mediaItems.isNotEmpty()) {
                    currentPlaylist = playlist

                    player.clearMediaItems()
                    val startIndex = if (startId != null) mediaItems.indexOfFirst { it.mediaId == startId } else -1
                    player.setMediaItems(mediaItems, if (startIndex == -1) C.INDEX_UNSET else startIndex, C.TIME_UNSET)
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    override suspend fun play() {
        controller?.let { player ->
            if (!player.isPlaying) player.play()
        }
    }

    override suspend fun pause() {
        controller?.let { player ->
            if (player.isPlaying) player.pause()
        }
    }

    override suspend fun stop() {

    }
}