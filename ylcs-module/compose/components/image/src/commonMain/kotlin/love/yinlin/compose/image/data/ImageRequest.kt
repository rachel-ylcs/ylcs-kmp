package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.input.ImageSource

@Stable
internal data class ImageRequest(
    val source: ImageSource,
    val version: String = "",
    val targetSize: IntSize? = null,
    val sizeMultiplier: Float = 1f,
    val scaleToFill: Boolean = false,
    val animated: Boolean = true,
    val diskCache: Boolean = true,
) {
    init {
        require(sizeMultiplier.isFinite() && sizeMultiplier > 0f)
        require(targetSize == null || (targetSize.width > 0 && targetSize.height > 0))
    }

    fun sourceKey(inputKey: String): String = "${inputKey.length}:$inputKey:$version"
    fun resultKey(sourceKey: String): String = "$sourceKey:${targetSize?.width}:${targetSize?.height}:$sizeMultiplier:$scaleToFill:$animated"
    fun decodeOptions(): ImageDecodeOptions = ImageDecodeOptions(targetSize, sizeMultiplier, scaleToFill, animated)
}
