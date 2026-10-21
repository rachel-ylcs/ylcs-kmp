package love.yinlin.compose.image.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.WebpInfo
import love.yinlin.extension.cleaning
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

@Stable
internal class AndroidWebpDecoder(
    private val bytes: ByteArray,
    private val info: WebpInfo,
    request: ImageDecodeOptions,
) : ImageDecoder {
    override val originalSize: IntSize = info.size
    override val size: IntSize = request.sampledSize(originalSize)
    override val durations: IntArray = if (request.animated) info.frames.map { it.duration }.toIntArray() else [info.frames[0].duration]
    override val repeatCount: Int = info.repeatCount
    private val bitmap = createBitmap(originalSize.width, originalSize.height)
    private val canvas = Canvas(bitmap)
    private val sourcePaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
    private val blendPaint = Paint()
    private val backgroundPaint = Paint().apply {
        color = info.background
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
    private var previous = -1

    override suspend fun frame(index: Int): ImageBitmap {
        require(index in durations.indices)
        val context = currentCoroutineContext()
        context.ensureActive()
        if (index <= previous) previous = -1
        if (previous == -1) canvas.drawColor(info.background, PorterDuff.Mode.SRC)

        for (position in previous + 1 .. index) {
            context.ensureActive()
            if (position > 0) {
                val before = info.frames[position - 1]
                if (before.dispose) canvas.drawRect(before.x.toFloat(), before.y.toFloat(),
                    (before.x + before.width).toFloat(), (before.y + before.height).toFloat(), backgroundPaint)
            }
            val frame = info.frames[position]
            val encoded = frame.encoded(bytes)
            context.ensureActive()
            val decoded = requireNotNull(BitmapFactory.decodeByteArray(encoded, 0, encoded.size)) { "Invalid WebP frame" }

            cleaning({
                context.ensureActive()
                require(decoded.width == frame.width && decoded.height == frame.height)
                canvas.drawBitmap(decoded, frame.x.toFloat(), frame.y.toFloat(), if (frame.blend) blendPaint else sourcePaint)
            }, decoded::recycle)
        }

        context.ensureActive()
        previous = index

        val result = if (size == originalSize) requireNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false)) else bitmap.scale(size.width, size.height)
        var transferred = false
        return cleaning({
            context.ensureActive()
            result.asImageBitmap().also { transferred = true }
        }) {
            if (!transferred) result.recycle()
        }
    }

    override fun close() = bitmap.recycle()
}
