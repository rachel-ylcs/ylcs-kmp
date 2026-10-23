package love.yinlin.compose.image.data

import androidx.compose.ui.graphics.FilterQuality
import love.yinlin.compose.data.ImageQuality

internal enum class ImageQualityPolicy(val filterQuality: FilterQuality) {
    Low(FilterQuality.Low),
    Medium(FilterQuality.Low),
    High(FilterQuality.Medium),
    Full(FilterQuality.High);

    companion object {
        fun from(quality: ImageQuality): ImageQualityPolicy = when (quality) {
            ImageQuality.Low -> Low
            ImageQuality.Medium -> Medium
            ImageQuality.High -> High
            ImageQuality.Full -> Full
        }
    }
}