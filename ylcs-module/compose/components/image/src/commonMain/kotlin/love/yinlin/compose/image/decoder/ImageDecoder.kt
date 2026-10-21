package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

/**
 * 仅由单一协程访问；frame 返回的位图所有权转移到 Compose，close 不回收已发布帧。
 * frame 取消后合成状态可能不完整，调用方必须关闭解码器，不再复用。
 */
@Stable
interface ImageDecoder : AutoCloseable {
    val originalSize: IntSize
    val size: IntSize
    val durations: IntArray
    val repeatCount: Int
    suspend fun frame(index: Int): ImageBitmap
}