package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.image.input.ImageSource

@Stable
internal data class ImageRequest(
    val source: ImageSource,
    val version: String? = null,
    val targetSize: IntSize? = null,
    val scaleToFill: Boolean = false,
    val animated: Boolean = true,
    val diskCache: Boolean = true,
    val quality: ImageQuality = ImageQuality.Medium,
    val hardwareAllowed: Boolean = false,
) {
    init {
        require(targetSize == null || (targetSize.width > 0 && targetSize.height > 0))
    }

    fun sourceKey(inputKey: String): String = if (version == null) "s:$inputKey" else "${inputKey.length}:$inputKey:$version"
    fun cacheGroup(sourceKey: String): String = "$sourceKey:$animated:$quality:$hardwareAllowed"
    fun resultKey(sourceKey: String): String = "${cacheGroup(sourceKey)}:${targetSize?.width}:${targetSize?.height}:$scaleToFill"
    fun decodeOptions(): ImageDecodeOptions = ImageDecodeOptions(targetSize, scaleToFill, animated, quality).also { it.hardwareAllowed = hardwareAllowed }
    fun sameContent(other: ImageRequest): Boolean = source == other.source && version == other.version && animated == other.animated && quality == other.quality && hardwareAllowed == other.hardwareAllowed
    fun canReuse(result: ImageResult): Boolean {
        val required = decodeOptions().sampledSize(result.originalSize)
        return result.size.width >= required.width && result.size.height >= required.height
    }
}
