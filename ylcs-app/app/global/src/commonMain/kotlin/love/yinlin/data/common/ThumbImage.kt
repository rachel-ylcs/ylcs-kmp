package love.yinlin.data.common

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.Picture

@Stable
@Serializable
data class ThumbImage(
    override val image: String,
    val source: String = image
) : Picture {
    override val isVideo: Boolean = false
}