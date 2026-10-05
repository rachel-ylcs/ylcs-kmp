package love.yinlin.foundation.parser.lrc

import kotlinx.serialization.Serializable

@Serializable
data class LrcMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val author: String? = null,
    val by: String? = null,
    val offset: Long = 0L,
    val editor: String? = null,
    val version: String? = null,
    val length: String? = null
)