package love.yinlin.data.compose

import androidx.compose.runtime.Stable

@Stable
data class ImageCropResult(
    val xPercent: Float,
    val yPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float
)