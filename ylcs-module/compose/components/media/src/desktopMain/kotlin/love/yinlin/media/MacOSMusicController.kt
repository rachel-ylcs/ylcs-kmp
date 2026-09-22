package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.annotation.NativeLibApi

@Stable
@NativeLibApi
internal class MacOSMusicController(fetcher: MediaMetadataFetcher) : MiniaudioMusicController(fetcher)
