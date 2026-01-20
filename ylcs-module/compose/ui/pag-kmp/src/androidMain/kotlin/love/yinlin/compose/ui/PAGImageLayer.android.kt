package love.yinlin.compose.ui

import love.yinlin.platform.unsupportedPlatform

actual class PAGImageLayer(override val delegate: PlatformPAGImageLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(width: Int, height: Int, duration: Long): PAGImageLayer =
            PAGImageLayer(PlatformPAGImageLayer.Make(width, height, duration))
    }

    actual val contentDuration: Long get() = delegate.contentDuration()
    actual fun replaceImage(image: PAGImage) = delegate.replaceImage(image.delegate)
    actual fun setImage(image: PAGImage) = delegate.setImage(image.delegate)
    actual fun layerTimeToContent(time: Long): Long = unsupportedPlatform()
    actual fun contentTimeToLayer(time: Long): Long = unsupportedPlatform()
}