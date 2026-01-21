package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

@Stable
actual class PAGSurface(internal val delegate: PlatformPAGSurface) {
    actual companion object {
        actual fun makeOffscreen(width: Int, height: Int): PAGSurface =
            PAGSurface(PlatformPAGSurface.MakeOffscreen(width, height))
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual fun updateSize() = delegate.updateSize()
    actual fun freeCache() = delegate.freeCache()
    actual fun clearAll() { delegate.clearAll() }
    actual fun makeSnapshot(): ImageBitmap? = delegate.makeSnapshot()?.asImageBitmap()
    actual fun close() = delegate.release()
}