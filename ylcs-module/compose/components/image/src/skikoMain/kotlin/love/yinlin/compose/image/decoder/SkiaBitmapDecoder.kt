package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.SamplingMode

@Stable
internal class SkiaBitmapDecoder(
    private val bitmap: Bitmap,
    override val originalSize: IntSize,
    override val size: IntSize,
    private val sampling: SamplingMode,
) : ImageDecoder {
    override val durations: IntArray = [0]
    override val repeatCount: Int = 0
    private var image: ImageBitmap? = null
    private var transferred = false

    override suspend fun frame(index: Int): ImageBitmap {
        require(index == 0)

        val context = currentCoroutineContext()
        context.ensureActive()

        image?.let { return it }

        val result = if (bitmap.width == size.width && bitmap.height == size.height) {
            bitmap.asComposeImageBitmap().also { transferred = true }
        }
        else bitmap.copyImageBitmap(size, context, sampling)

        image = result
        return result
    }

    override fun close() {
        if (!transferred) bitmap.close()
    }
}