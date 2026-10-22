package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import love.yinlin.extension.cleaning
import org.jetbrains.skia.AnimationDisposalMode
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedOrigin
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.coroutines.CoroutineContext

@Stable
internal class SkiaImageDecoder(
    private val data: Data?,
    private val codec: Codec?,
    private val staticImage: Image?,
    override val originalSize: IntSize,
    override val size: IntSize,
    animated: Boolean,
    private val origin: EncodedOrigin = EncodedOrigin.TOP_LEFT,
) : ImageDecoder {
    private val frames: Array<AnimationFrameInfo> = if (animated) requireNotNull(codec).framesInfo else []
    override val durations: IntArray = if (animated) frames.map { normalizedFrameDuration(it.duration) }.toIntArray() else [0]
    override val repeatCount: Int = if (animated) requireNotNull(codec).repetitionCount else 0

    private var scratch: Bitmap? = null
    private var lastFrame = -1

    override suspend fun frame(index: Int): ImageBitmap {
        require(index in durations.indices)
        val context = currentCoroutineContext()
        context.ensureActive()
        if (staticImage != null) return render(staticImage, context)

        val bitmap = scratch ?: Bitmap().also {
            scratch = it
            check(it.allocPixels(ImageInfo.makeN32Premul(originalSize.width, originalSize.height)))
        }
        val required = frames[index].requiredFrame
        val prior = if (lastFrame in 0 ..< index && required >= 0 && lastFrame >= required && frames[lastFrame].disposalMethod != AnimationDisposalMode.RESTORE_PREVIOUS) lastFrame else -1

        // 增量解码
        requireNotNull(codec).readPixels(bitmap, index, prior)
        context.ensureActive()
        lastFrame = index

        val pixels = requireNotNull(bitmap.peekPixels())

        return cleaning({
            val view = Data.makeWithoutCopy(pixels.addr, pixels.computeByteSize(), bitmap)
            cleaning({
                val image = Image.makeRaster(bitmap.imageInfo, view, bitmap.rowBytes)
                cleaning({
                    render(image, context)
                }, image::close)
            }, view::close)
        }, pixels::close)
    }

    private fun render(image: Image, context: CoroutineContext): ImageBitmap {
        val bitmap = Bitmap()
        var transferred = false

        return cleaning({
            check(bitmap.allocPixels(ImageInfo.makeN32Premul(size.width, size.height)))

            val canvas = Canvas(bitmap)
            cleaning({
                canvas.clear(0)
                val source = Rect.makeWH(image.width.toFloat(), image.height.toFloat())

                if (origin == EncodedOrigin.TOP_LEFT) {
                    canvas.drawImageRect(image, source, Rect.makeWH(size.width.toFloat(), size.height.toFloat()), SamplingMode.LINEAR, null, true)
                }
                else {
                    val rotated = origin.swapsWidthHeight()
                    val width = if (rotated) image.height else image.width
                    val height = if (rotated) image.width else image.height

                    canvas.scale(size.width.toFloat() / width, size.height.toFloat() / height)
                    canvas.concat(origin.toMatrix(width, height))
                    canvas.drawImageRect(image, source, source, SamplingMode.LINEAR, null, true)
                }
            }, canvas::close)

            context.ensureActive()
            bitmap.setImmutable()
            val imageBitmap = bitmap.asComposeImageBitmap()
            transferred = true
            imageBitmap
        }) {
            if (!transferred) bitmap.close()
        }
    }

    override fun close() {
        scratch?.close()
        staticImage?.close()
        codec?.close()
        data?.close()
    }
}
