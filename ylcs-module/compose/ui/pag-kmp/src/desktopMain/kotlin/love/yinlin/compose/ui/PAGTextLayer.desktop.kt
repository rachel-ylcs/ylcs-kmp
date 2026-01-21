package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Stable
actual class PAGTextLayer(override val delegate: PlatformPAGTextLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, text: String, fontSize: Float, font: PAGFont): PAGTextLayer =
            PAGTextLayer(PlatformPAGTextLayer.make(duration, text, fontSize, PlatformPAGFont(font.fontFamily, font.fontStyle)))
    }

    actual var fillColor: Color get() = Color(delegate.fillColor)
        set(value) { delegate.fillColor = value.toArgb() }
    actual var font: PAGFont get() = PAGFont(delegate.font.fontFamily, delegate.font.fontStyle)
        set(value) { delegate.font = PlatformPAGFont(value.fontFamily, value.fontStyle) }
    actual var fontSize: Float get() = delegate.fontSize
        set(value) { delegate.fontSize = value }
    actual var strokeColor: Color get() = Color(delegate.strokeColor)
        set(value) { delegate.strokeColor = value.toArgb() }
    actual var text: String get() = delegate.text
        set(value) { delegate.text = value }
    actual fun reset() = delegate.reset()
}