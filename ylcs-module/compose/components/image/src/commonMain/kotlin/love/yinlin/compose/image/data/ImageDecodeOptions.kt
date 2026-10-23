package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.image.decoder.sampledImageSize

@Stable
data class ImageDecodeOptions(
    val targetSize: IntSize? = null,
    val scaleToFill: Boolean = false,
    val animated: Boolean = true,
    val quality: ImageQuality = ImageQuality.Medium
) {
    internal var hardwareAllowed: Boolean = false
    internal var platformAnimationAllowed: Boolean = true

    init {
        require(targetSize == null || (targetSize.width > 0 && targetSize.height > 0))
    }

    fun sampledSize(original: IntSize): IntSize = sampledImageSize(original, this)

    internal fun staticOptions(): ImageDecodeOptions = if (animated) copy(animated = false).also {
        it.hardwareAllowed = hardwareAllowed
        it.platformAnimationAllowed = platformAnimationAllowed
    } else this
}
