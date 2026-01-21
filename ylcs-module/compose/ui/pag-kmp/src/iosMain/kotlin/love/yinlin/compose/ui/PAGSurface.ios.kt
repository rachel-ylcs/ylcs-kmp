@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import platform.CoreGraphics.CGSizeMake

@Stable
actual class PAGSurface(internal val delegate: PlatformPAGSurface) {
    actual companion object {
        actual fun makeOffscreen(width: Int, height: Int): PAGSurface =
            PAGSurface(PlatformPAGSurface.MakeOffscreen(CGSizeMake(width.toDouble(), height.toDouble()))!!)
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual fun updateSize() { delegate.updateSize() }
    actual fun freeCache() { delegate.freeCache() }
    actual fun clearAll() { delegate.clearAll() }
    actual fun makeSnapshot(): ImageBitmap? = delegate.makeSnapshot()?.let(::createImageFromPixelBuffer)
    actual fun close() { }
}