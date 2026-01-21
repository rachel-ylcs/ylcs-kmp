package love.yinlin.compose.ui

actual class PAGSurface(internal val delegate: PlatformPAGSurface) {
    actual companion object {
        actual fun makeOffscreen(width: Int, height: Int): PAGSurface =
            PAGSurface(PlatformPAGSurface.makeOffscreen(width, height))
    }

    actual val width: Int by delegate::width
    actual val height: Int by delegate::height
    actual fun updateSize() = delegate.updateSize()
    actual fun clearAll() = delegate.clearAll()
    actual fun close() = delegate.close()
}