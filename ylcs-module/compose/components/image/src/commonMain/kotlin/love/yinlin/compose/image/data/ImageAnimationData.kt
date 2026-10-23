package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize

@Stable
internal class ImageAnimationData(
    val bytes: ByteArray,
    val size: IntSize,
    val durations: IntArray,
    val repeatCount: Int, // -1 表示无限循环，0 表示只播放一次
    val request: ImageRequest,
    val platform: PlatformImageAnimation? = null,
) {
    fun forCache(): ImageAnimationData = platform?.let { ImageAnimationData(bytes, size, durations, repeatCount, request, it.cacheCopy()) } ?: this
}
