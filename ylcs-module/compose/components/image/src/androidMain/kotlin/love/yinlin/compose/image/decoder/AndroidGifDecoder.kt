package love.yinlin.compose.image.decoder

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.compose.image.data.GifInfo
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.extension.cleaning
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

@Stable
internal class AndroidGifDecoder(
    bytes: ByteArray,
    info: GifInfo,
    request: ImageDecodeOptions,
) : ImageDecoder {
    private val frames = GifFrames(bytes, info)
    override val originalSize: IntSize = info.size
    override val size: IntSize = request.sampledSize(originalSize)
    override val durations: IntArray = if (request.animated) info.durations else [info.durations[0]]
    override val repeatCount: Int = info.repeatCount
    private val bitmap = createBitmap(originalSize.width, originalSize.height)

    override suspend fun frame(index: Int): ImageBitmap {
        require(index in durations.indices)
        val context = currentCoroutineContext()
        val pixels = frames.frame(index, context)
        context.ensureActive()
        bitmap.setPixels(pixels, 0, originalSize.width, 0, 0, originalSize.width, originalSize.height)
        context.ensureActive()

        val scaled = if (size == originalSize) requireNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false)) else bitmap.scale(size.width, size.height)
        var transferred = false
        return cleaning({
            context.ensureActive()
            scaled.prepareToDraw()
            scaled.asImageBitmap().also { transferred = true }
        }) {
            if (!transferred) scaled.recycle()
        }
    }

    override fun close() = bitmap.recycle()
}
