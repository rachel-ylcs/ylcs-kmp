@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.platform.unsupportedPlatform
import platform.CoreGraphics.CGSizeMake

actual class PAGImageLayer(private val delegate: PlatformPAGImageLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(width: Int, height: Int, duration: Long): PAGImageLayer =
            PAGImageLayer(PlatformPAGImageLayer.Make(CGSizeMake(width.toDouble(), height.toDouble()), duration)!!)
    }

    actual val contentDuration: Long get() = delegate.contentDuration()
    actual fun replaceImage(image: PAGImage) { delegate.replaceImage(image.delegate) }
    actual fun setImage(image: PAGImage) { delegate.setImage(image.delegate) }
    actual fun layerTimeToContent(time: Long): Long = unsupportedPlatform()
    actual fun contentTimeToLayer(time: Long): Long = unsupportedPlatform()
}