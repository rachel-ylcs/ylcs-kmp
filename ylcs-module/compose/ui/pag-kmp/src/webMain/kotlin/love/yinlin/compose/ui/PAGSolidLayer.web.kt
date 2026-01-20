package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Color

actual class PAGSolidLayer(override val delegate: PlatformPAGSolidLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, width: Int, height: Int, solidColor: Color, opacity: Int): PAGSolidLayer {
            return PAGSolidLayer(PlatformPAGSolidLayer.make(duration.toDouble(), width, height, solidColor.asPAGColor(), opacity.toDouble()))
        }
    }

    actual var solidColor: Color get() = delegate.solidColor().asComposeColor()
        set(value) { delegate.setSolidColor(value.asPAGColor()) }
}