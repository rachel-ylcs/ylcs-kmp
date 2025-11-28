package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toSkiaRect
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import love.yinlin.data.MimeType
import love.yinlin.extension.catchingNull
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

@Stable
actual class AnimatedWebp internal constructor(
    actual val frameCount: Int,
    private val codec: Codec
) {
    private val info: ImageInfo = codec.imageInfo

    // https://github.com/coil-kt/coil/pull/2594
    private fun Bitmap.useCache(): Bitmap {
        allocPixels(info)
        setImmutable()
        return this
    }
    private val cacheA: Bitmap = Bitmap().useCache()
    private val cacheB: Bitmap = Bitmap().useCache()
    private var useCacheA: Boolean by mutableStateOf(false)
    private val lock = SynchronizedObject()

    private val paint: Paint = Paint().apply { isAntiAlias = true }

    actual val width: Int = info.width
    actual val height: Int = info.height

    private var frame: Int = -1

    actual suspend fun nextFrame() {
        synchronized(lock) {
            if (frame >= frameCount - 1) frame = 0
            else ++frame
            (if (useCacheA) cacheA else cacheB).let { cache ->
                codec.readPixels( cache, frame)
                cache.notifyPixelsChanged()
            }
            useCacheA = !useCacheA
        }
    }

    actual fun resetFrame() {
        synchronized(lock) {
            frame = -1
            cacheA.reset()
            cacheA.useCache()
            cacheA.notifyPixelsChanged()
            cacheB.reset()
            cacheB.useCache()
            cacheB.notifyPixelsChanged()
            useCacheA = !useCacheA
        }
    }

    actual fun DrawScope.drawFrame(dst: Rect, src: Rect?) {
        drawIntoCanvas { canvas ->
            Image.makeFromBitmap(if (useCacheA) cacheA else cacheB).use { image ->
                canvas.nativeCanvas.drawImageRect(
                    image = image,
                    src = src?.toSkiaRect() ?: org.jetbrains.skia.Rect.makeWH(width.toFloat(), height.toFloat()),
                    dst = dst.toSkiaRect(),
                    paint = paint,
                    samplingMode = SamplingMode.MITCHELL,
                    strict = true
                )
            }
        }
    }

    actual fun release() {
        if (!paint.isClosed) paint.close()
        if (!cacheA.isClosed) cacheA.close()
        if (!cacheB.isClosed) cacheB.close()
        if (!codec.isClosed) codec.close()
    }

    actual companion object {
        actual fun decode(data: ByteArray): AnimatedWebp? = catchingNull {
            require(checkHeader(data))
            Data.makeFromBytes(data).use { rawData ->
                val codec = Codec.makeFromData(rawData)
                val info = codec.imageInfo
                val frameCount = codec.frameCount
                val mimeType = "image/${codec.encodedImageFormat.name.lowercase()}"
                require(info.width > 0 && info.height > 0 && frameCount > 1 && mimeType == MimeType.WEBP)
                AnimatedWebp(frameCount, codec)
            }
        }
    }
}