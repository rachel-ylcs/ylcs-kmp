@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.graphics.asComposeColor
import love.yinlin.compose.graphics.asUIColor
import love.yinlin.platform.unsupportedPlatform

@Stable
actual class PAGSolidLayer(override val delegate: PlatformPAGSolidLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, width: Int, height: Int, solidColor: Color, opacity: Int): PAGSolidLayer = unsupportedPlatform()
    }

    actual var solidColor: Color get() = delegate.solidColor()!!.asComposeColor()
        set(value) { delegate.setSolidColor(value.asUIColor()) }
}