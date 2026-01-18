package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asAndroidMatrix
import love.yinlin.compose.graphics.asComposeMatrix
import androidx.core.graphics.createBitmap

actual class PAGImage(private val delegate: PlatformPAGImage) {
    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode()]
        set(value) { delegate.setScaleMode(value.ordinal) }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asAndroidMatrix()) }

    actual fun close() = delegate.release()

    actual companion object {
        actual fun loadFromPath(path: String): PAGImage = PAGImage(PlatformPAGImage.FromPath(path))
        actual fun loadFromBytes(bytes: ByteArray): PAGImage = PAGImage(PlatformPAGImage.FromBytes(bytes))
        actual fun loadFromPixels(pixels: IntArray, width: Int, height: Int, rowBytes: Long, colorType: PAGColorType, alphaType: PAGAlphaType): PAGImage {
            val config = colorType.asAndroidBitmapConfig
            val bitmap = createBitmap(width, height, config)
            when (alphaType) {
                PAGAlphaType.UNPREMULTIPLIED -> bitmap.isPremultiplied = false
                PAGAlphaType.OPAQUE -> bitmap.setHasAlpha(false)
                else -> bitmap.isPremultiplied = true
            }
            val bytesPerPixel = 4
            val strideInPixels = (rowBytes / bytesPerPixel).toInt()
            bitmap.setPixels(pixels, 0, strideInPixels, 0, 0, width, height)
            val image = PlatformPAGImage.FromBitmap(bitmap)
            bitmap.recycle()
            return PAGImage(image)
        }
    }
}