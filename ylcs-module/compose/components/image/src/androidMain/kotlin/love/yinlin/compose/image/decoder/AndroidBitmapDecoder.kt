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
    override val size: IntSize = IntSize(bitmap.width, bitmap.height)
    override val durations: IntArray = [0]
    override val repeatCount: Int = 0
    override suspend fun frame(index: Int): ImageBitmap {
        require(index == 0)
        currentCoroutineContext().ensureActive()
        return bitmap.asImageBitmap()
    }
    override fun close() { } // 位图所有权已交给 Compose，不能在 loader 的 finally 中 recycle
}