package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContext

@Stable
class WebMusicPlayer(fetcher: MediaMetadataFetcher) : CommonMusicPlayer(fetcher) {
    override suspend fun init(context: PlatformContext) {}
    override fun release() {}
    override suspend fun play() {}
    override suspend fun pause() {}
    override suspend fun seekTo(position: Long) {}
    override fun innerStop() {}
    override fun innerGotoIndex(path: String, playing: Boolean): Boolean = false
}

actual fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer = WebMusicPlayer(fetcher)