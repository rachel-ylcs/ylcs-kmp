package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMedia

@Stable
@Serializable
sealed interface BilibiliMedia : UnifiedMedia {
    @Stable
    @Serializable
    data class Image(override val image: String) : BilibiliMedia {
        override val isVideo: Boolean = false
    }

    @Stable
    @Serializable
    data class Video(override val image: String, val aid: String, val bvid: String) : BilibiliMedia {
        override val isVideo: Boolean = true
    }
}