package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import love.yinlin.extension.catchingNull
import love.yinlin.platform.unsupportedPlatform
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.khronos.webgl.toUByteArray

@Stable
actual class PAGSurface(internal val delegate: PlatformPAGSurface) {
    actual companion object {
        actual fun makeOffscreen(width: Int, height: Int): PAGSurface = unsupportedPlatform()
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual fun updateSize() = delegate.updateSize()
    actual fun freeCache() = delegate.freeCache()
    actual fun clearAll() { delegate.clearAll() }
    @OptIn(ExperimentalUnsignedTypes::class)
    actual fun makeSnapshot(): ImageBitmap? = catchingNull {
        val data = delegate.readPixels(PAGColorType.RGBA_8888.ordinal, PAGAlphaType.PREMULTIPLIED.ordinal)!!.toUByteArray().toByteArray()
        val imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
        val bitmap = Bitmap()
        bitmap.installPixels(imageInfo, data, imageInfo.minRowBytes)
        bitmap.asComposeImageBitmap()
    }
    actual fun close() = delegate.destroy()
}