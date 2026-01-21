package love.yinlin.compose.ui

actual class PAGDecoder(private val delegate: PlatformPAGDecoder) {
    actual companion object {
        actual fun makeFrom(composition: PAGComposition, maxFrameRate: Float, scale: Float): PAGDecoder =
            PAGDecoder(PlatformPAGDecoder.Make(composition.delegate, maxFrameRate, scale))
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual val numFrames: Int get() = delegate.numFrames()
    actual val frameRate: Float get() = delegate.frameRate()
    actual fun checkFrameRate(index: Int): Boolean = delegate.checkFrameChanged(index)
    actual fun close() = delegate.release()
}