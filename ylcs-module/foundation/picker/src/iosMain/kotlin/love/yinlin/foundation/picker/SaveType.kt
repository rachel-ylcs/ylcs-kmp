package love.yinlin.foundation.picker

import platform.Foundation.NSURL

internal sealed interface SaveType {
    val filename: String
    val url: NSURL

    data class Photo(override val filename: String, override val url: NSURL) : SaveType
    data class Video(override val filename: String, override val url: NSURL) : SaveType
}