package love.yinlin.compose.ui

actual class PAGDecoder(private val delegate: PlatformPAGDecoder) {
    actual companion object {
        actual fun makeFrom(composition: PAGComposition, maxFrameRate: Float, scale: Float): PAGDecoder =
            PAGDecoder(PlatformPAGDecoder.makeFrom(composition.delegate, maxFrameRate, scale))
    }

    actual val width: Int by delegate::width
    actual val height: Int by delegate::height
    actual val numFrames: Int by delegate::numFrames
    actual val frameRate: Float by delegate::frameRate
    actual fun checkFrameRate(index: Int): Boolean = delegate.checkFrameRate(index)
    actual fun close() = delegate.close()
}