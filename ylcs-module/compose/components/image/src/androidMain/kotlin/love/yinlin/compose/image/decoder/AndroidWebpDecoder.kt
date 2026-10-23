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
import love.yinlin.extension.catchingError

@Stable
internal class AndroidWebpDecoder(
    private val bytes: ByteArray,
    private val info: WebpInfo,
    request: ImageDecodeOptions,
) : ImageDecoder {
    override val originalSize: IntSize = info.size
    override val size: IntSize = request.sampledSize(originalSize)
    override val durations: IntArray = if (request.animated) IntArray(info.frames.size) { info.frames[it].duration } else [info.frames[0].duration]
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
    private val encoded = ByteArray(info.frames.maxOf { it.encodedSize })
    private val decodeOptions = BitmapFactory.Options().apply {
        inMutable = true
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    private var decodedFrame: Bitmap? = null

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
            frame.encodeInto(bytes, encoded)

            context.ensureActive()

            val reusable = decodedFrame?.takeIf { it.allocationByteCount.toLong() >= frame.width.toLong() * frame.height * 4L }
            decodeOptions.inBitmap = reusable

            var decoded: Bitmap? = null
            val error = catchingError {
                decoded = BitmapFactory.decodeByteArray(encoded, 0, frame.encodedSize, decodeOptions)
            }

            if (error != null) {
                if (reusable == null || error !is IllegalArgumentException) throw error
                decodeOptions.inBitmap = null
                decoded = BitmapFactory.decodeByteArray(encoded, 0, frame.encodedSize, decodeOptions)
            }
            val current = requireNotNull(decoded) { "Invalid WebP frame" }

            if (decodedFrame !== current) decodedFrame?.recycle()
            decodedFrame = current

            context.ensureActive()

            require(current.width == frame.width && current.height == frame.height)

            canvas.drawBitmap(current, frame.x.toFloat(), frame.y.toFloat(), if (frame.blend) blendPaint else sourcePaint)
        }

        context.ensureActive()
        previous = index

        val result = if (size == originalSize) requireNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false)) else bitmap.scale(size.width, size.height)
        var transferred = false
        return cleaning({
            context.ensureActive()
            result.prepareToDraw()
            result.asImageBitmap().also { transferred = true }
        }) {
            if (!transferred) result.recycle()
        }
    }

    override fun close() {
        decodeOptions.inBitmap = null
        decodedFrame?.recycle()
        bitmap.recycle()
    }
}
