package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.foundation.Context

@Stable
class WebMusicPlayer(fetcher: MediaMetadataFetcher) : MusicPlayer(fetcher) {
    override suspend fun init(context: Context) { }
    override fun release() { }
    override suspend fun updatePlayMode(mode: MediaPlayMode) { }
    override suspend fun play() { }
    override suspend fun pause() { }
    override suspend fun stop() { }
    override suspend fun gotoPrevious() { }
    override suspend fun gotoNext() { }
    override suspend fun gotoIndex(index: Int) { }
    override suspend fun seekTo(position: Long) { }
    override suspend fun prepareMedias(medias: List<String>, startIndex: Int?, playing: Boolean) { }
    override suspend fun addMedias(medias: List<String>) { }
    override suspend fun removeMedia(index: Int) { }
}

actual fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer = WebMusicPlayer(fetcher)