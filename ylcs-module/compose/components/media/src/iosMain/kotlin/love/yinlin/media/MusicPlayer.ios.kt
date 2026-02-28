package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.foundation.Context

// TODO: 需要重新实现, 如果原生库不支持播放列表可以参考 desktop 直接继承自 CommonMusicPlayer, 否则参考 android
@Stable
class IOSMusicPlayer<Info : MediaInfo>(fetcher: MediaMetadataFetcher<Info>) : MusicPlayer<Info>(fetcher) {
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
    override suspend fun prepareMedias(medias: List<Info>, startIndex: Int?, playing: Boolean) { }
    override suspend fun addMedias(medias: List<Info>) { }
    override suspend fun removeMedia(index: Int) { }
}

actual fun <Info : MediaInfo> buildMusicPlayer(fetcher: MediaMetadataFetcher<Info>): MusicPlayer<Info> = IOSMusicPlayer(fetcher)