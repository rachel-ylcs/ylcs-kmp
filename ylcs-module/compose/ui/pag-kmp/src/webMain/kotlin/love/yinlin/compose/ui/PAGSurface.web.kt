package love.yinlin.compose.ui

import love.yinlin.platform.unsupportedPlatform

actual class PAGSurface(internal val delegate: PlatformPAGSurface) {
    actual companion object {
        actual fun makeOffscreen(width: Int, height: Int): PAGSurface = unsupportedPlatform()
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual fun updateSize() = delegate.updateSize()
    actual fun clearAll() { delegate.clearAll() }
    actual fun close() = delegate.destroy()
}