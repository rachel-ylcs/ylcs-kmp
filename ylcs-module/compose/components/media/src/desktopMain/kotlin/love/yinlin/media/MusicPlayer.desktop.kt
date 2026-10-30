package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.foundation.LibLoader
import love.yinlin.foundation.NativeLibApi
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Stable
@NativeLibApi
internal object DesktopMusicController {
    init {
        LibLoader.resource("ylcs-module.compose.components.media")
    }

    fun build(fetcher: MediaMetadataFetcher): MusicPlayer = when (platform) {
        Platform.Windows -> WindowsMusicController(fetcher)
        else -> MiniaudioMusicController(fetcher)
    }
}

actual fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer = DesktopMusicController.build(fetcher)