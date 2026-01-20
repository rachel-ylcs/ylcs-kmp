package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

actual class PAGTextLayer(private val delegate: PlatformPAGTextLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, text: String, fontSize: Float, font: PAGFont): PAGTextLayer =
            PAGTextLayer(PlatformPAGTextLayer.make(duration, text, fontSize, PlatformPAGFont(font.fontFamily, font.fontStyle)))
    }

    actual var fillColor: Color get() = Color(delegate.fillColor)
        set(value) { delegate.fillColor = value.toArgb() }
    actual var font: PAGFont get() = PAGFont(delegate.font.fontFamily, delegate.font.fontStyle)
        set(value) { delegate.font = PlatformPAGFont(value.fontFamily, value.fontStyle) }
    actual var fontSize: Float by delegate::fontSize
    actual var strokeColor: Color get() = Color(delegate.strokeColor)
        set(value) { delegate.strokeColor = value.toArgb() }
    actual var text: String by delegate::text
    actual fun reset() = delegate.reset()
}