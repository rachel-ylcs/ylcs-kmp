package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import love.yinlin.platform.unsupportedPlatform

actual class PAGTextLayer(private val delegate: PlatformPAGTextLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, text: String, fontSize: Float, font: PAGFont): PAGTextLayer = unsupportedPlatform()
    }

    actual var fillColor: Color get() = Color(delegate.fillColor())
        set(value) { delegate.setFillColor(value.toArgb()) }
    actual var font: PAGFont get() = delegate.font().let { PAGFont(it.fontFamily, it.fontStyle) }
        set(value) { delegate.setFont(PlatformPAGFont(value.fontFamily, value.fontStyle)) }
    actual var fontSize: Float get() = delegate.fontSize()
        set(value) { delegate.setFontSize(value) }
    actual var strokeColor: Color get() = Color(delegate.strokeColor())
        set(value) { delegate.setStrokeColor(value.toArgb()) }
    actual var text: String get() = delegate.text()
        set(value) { delegate.setText(value) }
    actual fun reset() = delegate.reset()
}