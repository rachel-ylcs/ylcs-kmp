package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import love.yinlin.platform.unsupportedPlatform

actual class PAGSolidLayer(override val delegate: PlatformPAGSolidLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, width: Int, height: Int, solidColor: Color, opacity: Int): PAGSolidLayer = unsupportedPlatform()
    }

    actual var solidColor: Color get() = Color(delegate.solidColor())
        set(value) { delegate.setSolidColor(value.toArgb()) }
}