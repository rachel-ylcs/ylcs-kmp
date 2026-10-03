package love.yinlin.data.radio

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMedia

@Stable
@Serializable
data class RadioMedia(override val image: String) : UnifiedMedia {
    override val isVideo: Boolean = false
}