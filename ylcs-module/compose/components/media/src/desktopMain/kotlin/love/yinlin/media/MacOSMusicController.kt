package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.Context

@Stable
@NativeLibApi
internal class MacOSMusicController(fetcher: MediaMetadataFetcher) : CommonMusicPlayer(fetcher) {
    override suspend fun init(context: Context) { }
    override fun release() { }
    override suspend fun play() { }
    override suspend fun pause() { }
    override suspend fun seekTo(position: Long) { }
    override fun innerGotoIndex(index: Int, playing: Boolean) { }
    override fun innerStop() { }
}