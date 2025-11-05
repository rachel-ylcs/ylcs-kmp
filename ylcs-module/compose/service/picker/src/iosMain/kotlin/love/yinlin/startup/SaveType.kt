package love.yinlin.startup

import androidx.compose.runtime.Stable
import platform.Foundation.NSURL

@Stable
internal sealed interface SaveType {
    @Stable
    data class Photo(val filename: String) : SaveType
    @Stable
    data class Video(val filename: String, val url: NSURL) : SaveType
}