package love.yinlin.compose.image.decoder

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@Stable
internal class AndroidBitmapDecoder(private val bitmap: Bitmap, override val originalSize: IntSize) : ImageDecoder {
    private var image: ImageBitmap? = null

    override val size: IntSize = IntSize(bitmap.width, bitmap.height)
    override val durations: IntArray = [0]
    override val repeatCount: Int = 0
    override suspend fun frame(index: Int): ImageBitmap {
        require(index == 0)
        currentCoroutineContext().ensureActive()

        image?.let { return it }
        bitmap.prepareToDraw()

        val imageBitmap = bitmap.asImageBitmap()
        image = imageBitmap
        return imageBitmap
    }

    override fun close() {
        // 取消发生在首次移交之前时立即释放；已发布位图的所有权留给 Compose
        if (image == null && !bitmap.isRecycled) bitmap.recycle()
    }
}