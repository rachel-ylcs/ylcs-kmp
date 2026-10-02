package love.yinlin.data.douyin

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMedia

@Stable
@Serializable
sealed interface DouyinMedia : UnifiedMedia {
    @Stable
    @Serializable
    data class Image(override val image: String) : DouyinMedia {
        override val isVideo: Boolean = false
    }

    @Stable
    @Serializable
    data class Video(override val image: String, val videoList: List<String>) : DouyinMedia {
        override val isVideo: Boolean = true
    }
}