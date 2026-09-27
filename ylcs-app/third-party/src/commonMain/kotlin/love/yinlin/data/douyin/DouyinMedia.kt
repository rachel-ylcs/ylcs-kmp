package love.yinlin.data.douyin

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMedia

@Stable
@Serializable
sealed interface DouyinMedia : UnifiedMedia {
    @Stable
    @Serializable
    data class Image(val image: String) : DouyinMedia

    @Stable
    @Serializable
    data class Video(val cover: String, val video: List<String>) : DouyinMedia
}