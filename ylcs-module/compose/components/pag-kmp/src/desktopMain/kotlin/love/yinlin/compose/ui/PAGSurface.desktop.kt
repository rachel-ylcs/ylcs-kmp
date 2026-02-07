package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import love.yinlin.extension.catchingNull
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

@Stable
actual class PAGSurface(internal val delegate: PlatformPAGSurface) {
    actual companion object {
        actual fun makeOffscreen(width: Int, height: Int): PAGSurface =
            PAGSurface(PlatformPAGSurface.makeOffscreen(width, height))
    }

    private var bitmap = Bitmap()
    private var buffer = byteArrayOf()

    actual val width: Int get() = delegate.width
    actual val height: Int get() = delegate.height
    actual fun updateSize() = delegate.updateSize()
    actual fun freeCache() = delegate.freeCache()
    actual fun clearAll() = delegate.clearAll()
    actual fun makeSnapshot(): ImageBitmap? {
        val imageWidth = width
        val imageHeight = height
        val rowBytes = imageWidth * 4
        return catchingNull {
            if (imageWidth != bitmap.width || imageHeight != bitmap.height) {
                bitmap.allocPixels(ImageInfo(imageWidth, imageHeight, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
                buffer = ByteArray(rowBytes * imageHeight)
            }
            val result = delegate.readPixels(PAGColorType.RGBA_8888.ordinal, PAGAlphaType.PREMULTIPLIED.ordinal, rowBytes.toLong(), buffer)
            require(result)
            bitmap.installPixels(buffer)
            bitmap.asComposeImageBitmap()
        }
    }
    actual fun close() {
        delegate.close()
    }
}