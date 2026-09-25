package love.yinlin.data.information

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class UnifiedPicture(
    val image: String,
    val source: String = image,
    val video: String = ""
) {
    val isImage: Boolean = video.isEmpty()
    val isVideo: Boolean = video.isNotEmpty()
}