package love.yinlin.media

import android.content.ComponentName
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.coroutines.Coroutines
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.extension.catchingNull
import love.yinlin.foundation.Context

@Stable
class AndroidMusicPlayer<Info : MediaInfo>(fetcher: MediaMetadataFetcher<Info>) : MusicPlayer<Info>(fetcher) {
    private var controller: MediaController? by mutableRefStateOf(null)

    private val androidListener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) {

        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {

        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {

        }

        override fun onPlaybackStateChanged(playbackState: Int) {

        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

        }

        override fun onPlayerError(error: PlaybackException) {

        }
    }

    override suspend fun init(context: Context) {
        val ctx = context.application
        val (pkg, cls) = fetcher.androidMusicServiceComponentName
        val mediaController = catchingNull {
            Coroutines.main {
                val token = SessionToken(ctx, ComponentName(pkg, cls))
                val bitmapLoader = DataSourceBitmapLoader.Builder(ctx).setMakeShared(true).build()
                MediaController.Builder(ctx, token).setBitmapLoader(bitmapLoader).buildAsync().get()
            }
        }
        if (mediaController != null) {
            mediaController.removeListener(androidListener)
            mediaController.addListener(androidListener)
            controller = mediaController
        }
    }

    override fun release() {

    }

    override suspend fun updatePlayMode(mode: MediaPlayMode) {

    }

    override suspend fun play() {

    }

    override suspend fun pause() {

    }

    override suspend fun stop() {

    }

    override suspend fun gotoPrevious() {

    }

    override suspend fun gotoNext() {

    }

    override suspend fun gotoIndex(index: Int) {

    }

    override suspend fun seekTo(position: Long) {

    }

    override suspend fun prepareMedias(medias: List<Info>, startIndex: Int?, playing: Boolean) {

    }

    override suspend fun addMedias(medias: List<Info>) {

    }

    override suspend fun removeMedia(index: Int) {

    }
}

actual fun <Info : MediaInfo> buildMusicPlayer(fetcher: MediaMetadataFetcher<Info>): MusicPlayer<Info> = AndroidMusicPlayer(fetcher)