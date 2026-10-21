package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.decoder.sampledImageSize

@Stable
data class ImageDecodeOptions(
    val targetSize: IntSize? = null,
    val sizeMultiplier: Float = 1f,
    val scaleToFill: Boolean = false,
    val animated: Boolean = true,
) {
    init {
        require(sizeMultiplier.isFinite() && sizeMultiplier > 0f)
        require(targetSize == null || (targetSize.width > 0 && targetSize.height > 0))
    }

    fun sampledSize(original: IntSize): IntSize = sampledImageSize(original, this)
}
