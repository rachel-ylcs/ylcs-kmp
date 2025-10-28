package love.yinlin.startup

import platform.Foundation.NSURL

internal sealed interface SaveType {
    data class Photo(val filename: String) : SaveType
    data class Video(val filename: String, val url: NSURL) : SaveType
}