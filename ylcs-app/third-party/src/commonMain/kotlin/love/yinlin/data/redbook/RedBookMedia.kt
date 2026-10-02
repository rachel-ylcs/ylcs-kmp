package love.yinlin.data.redbook

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMedia

@Stable
@Serializable
sealed interface RedBookMedia : UnifiedMedia {
    @Stable
    @Serializable
    data class Image(override val image: String) : RedBookMedia {
        override val isVideo: Boolean = false
    }

    @Stable
    @Serializable
    data class Video(override val image: String) : RedBookMedia {
        override val isVideo: Boolean = true
    }
}