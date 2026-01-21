package love.yinlin.compose.ui

actual class PAGSurface(internal val delegate: PlatformPAGSurface) {
    actual companion object {
        actual fun makeOffscreen(width: Int, height: Int): PAGSurface =
            PAGSurface(PlatformPAGSurface.MakeOffscreen(width, height))
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual fun updateSize() = delegate.updateSize()
    actual fun clearAll() { delegate.clearAll() }
    actual fun close() = delegate.release()
}