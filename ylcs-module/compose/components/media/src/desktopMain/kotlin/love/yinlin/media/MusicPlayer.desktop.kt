package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.platform.NativeLibLoader
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Stable
@NativeLibApi
internal object DesktopAudioController {
    init {
        NativeLibLoader.resource("media")
    }

    fun <Info : MediaInfo> build(fetcher: MediaMetadataFetcher<Info>): MusicPlayer<Info> = when (platform) {
        Platform.Windows -> WindowsMusicController(fetcher)
        Platform.MacOS -> MacOSMusicController(fetcher)
        else -> LinuxMusicController(fetcher)
    }
}

actual fun <Info : MediaInfo> buildMusicPlayer(fetcher: MediaMetadataFetcher<Info>): MusicPlayer<Info> = DesktopAudioController.build(fetcher)