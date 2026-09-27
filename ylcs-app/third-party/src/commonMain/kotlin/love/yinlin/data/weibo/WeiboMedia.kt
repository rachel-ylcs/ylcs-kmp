package love.yinlin.data.weibo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMedia

@Stable
@Serializable
sealed interface WeiboMedia : UnifiedMedia {
    @Stable
    @Serializable
    data class Image(val image: String, val source: String) : WeiboMedia

    @Stable
    @Serializable
    data class Video(val cover: String, val video: String) : WeiboMedia
}