package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Stable
@NativeLibApi
internal object DesktopMusicController {
    init {
        NativeLibLoader.resource("media")
    }

    fun build(fetcher: MediaMetadataFetcher): MusicPlayer = when (platform) {
        Platform.Windows -> WindowsMusicController(fetcher)
        Platform.MacOS -> MacOSMusicController(fetcher)
        else -> LinuxMusicController(fetcher)
    }
}

actual fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer = DesktopMusicController.build(fetcher)