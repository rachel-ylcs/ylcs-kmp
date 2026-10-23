package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.decoder.ImageFormat
import love.yinlin.compose.image.input.ImageInput
import love.yinlin.fs.File

@Stable
data class ImageConfiguration(
    val cacheDirectory: File? = null,
    val maxDiskCacheBytes: Long = 256L * 1024 * 1024,
    val maxMemoryCacheBytes: Long = 64L * 1024 * 1024,
    val maxSourceBytes: Int = 64 * 1024 * 1024,
    val maxDecodePixels: Long = 64L * 1024 * 1024,
    val loadingDelayMillis: Long = 500L,
    val inputs: List<ImageInput<*>> = [],
    val formats: List<ImageFormat> = [],
) {
    init {
        require(maxDiskCacheBytes >= 0L && maxMemoryCacheBytes >= 0L)
        require(maxSourceBytes > 0)
        require(maxDecodePixels in 1L .. 64L * 1024 * 1024)
        require(loadingDelayMillis >= 0L)
    }
}
