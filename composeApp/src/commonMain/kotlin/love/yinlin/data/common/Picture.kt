package love.yinlin.data.common

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class Picture(
	val image: String,
	val source: String = image,
	val video: String = ""
) {
	val isImage: Boolean get() = video.isEmpty()
	val isVideo: Boolean get() = video.isNotEmpty()
}