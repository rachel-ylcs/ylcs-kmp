package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ColorFilter
import love.yinlin.compose.data.ImageQuality

@Stable
data class ImageOptions(
    val animated: Boolean = true,
    val zoom: Boolean = false,
    val circular: Boolean = false,
    val quality: ImageQuality = ImageQuality.Medium,
    val colorFilter: ColorFilter? = null,
    val fadeIn: Boolean = false,
) {
    companion object {
        val Default: ImageOptions = ImageOptions()
    }
}
