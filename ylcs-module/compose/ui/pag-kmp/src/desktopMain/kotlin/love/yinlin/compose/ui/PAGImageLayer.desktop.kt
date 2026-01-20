package love.yinlin.compose.ui

actual class PAGImageLayer(private val delegate: PlatformPAGImageLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(width: Int, height: Int, duration: Long): PAGImageLayer =
            PAGImageLayer(PlatformPAGImageLayer.make(width, height, duration))
    }

    actual val contentDuration: Long by delegate::contentDuration
    actual fun replaceImage(image: PAGImage) = delegate.replaceImage(image.delegate)
    actual fun setImage(image: PAGImage) = delegate.setImage(image.delegate)
    actual fun layerTimeToContent(time: Long): Long = delegate.layerTimeToContent(time)
    actual fun contentTimeToLayer(time: Long): Long = delegate.contentTimeToLayer(time)
}