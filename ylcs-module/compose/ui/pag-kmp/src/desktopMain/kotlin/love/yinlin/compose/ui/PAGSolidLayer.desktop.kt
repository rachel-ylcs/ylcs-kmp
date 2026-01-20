package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

actual class PAGSolidLayer(override val delegate: PlatformPAGSolidLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, width: Int, height: Int, solidColor: Color, opacity: Int): PAGSolidLayer {
            return PAGSolidLayer(PlatformPAGSolidLayer.make(duration, width, height, solidColor.toArgb(), opacity))
        }
    }

    actual var solidColor: Color get() = Color(delegate.solidColor)
        set(value) { delegate.solidColor = value.toArgb() }
}