package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual class PAGImageLayer(override val delegate: PlatformPAGImageLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(width: Int, height: Int, duration: Long): PAGImageLayer =
            PAGImageLayer(PlatformPAGImageLayer.make(width, height, duration.toDouble()))
    }

    actual val contentDuration: Long get() = delegate.contentDuration().toLong()
    actual fun replaceImage(image: PAGImage) = delegate.replaceImage(image.delegate)
    actual fun setImage(image: PAGImage) = delegate.setImage(image.delegate)
    actual fun layerTimeToContent(time: Long): Long = delegate.layerTimeToContent(time.toDouble()).toLong()
    actual fun contentTimeToLayer(time: Long): Long = delegate.contentTimeToLayer(time.toDouble()).toLong()
}