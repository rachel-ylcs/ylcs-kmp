package love.yinlin.data.redbook

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMedia

@Stable
@Serializable
sealed interface RedBookMedia : UnifiedMedia {
    @Stable
    @Serializable
    data class Image(val image: String, val source: String) : RedBookMedia

    @Stable
    @Serializable
    data class Video(val cover: String, val video: String) : RedBookMedia
}