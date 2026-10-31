package love.yinlin.media

import androidx.compose.runtime.Stable

@Stable
internal class MiniaudioMusicPlayer(fetcher: MediaMetadataFetcher) : PcMusicPlayer(fetcher) {
    override fun nativePlaybackStateChange(value: Int) { updatePlayback(value != 0) }
}
