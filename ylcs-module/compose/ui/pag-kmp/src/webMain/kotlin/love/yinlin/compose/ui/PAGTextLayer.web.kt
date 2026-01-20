package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Color

actual class PAGTextLayer(override val delegate: PlatformPAGTextLayer) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(duration: Long, text: String, fontSize: Float, font: PAGFont): PAGTextLayer =
            PAGTextLayer(PlatformPAGTextLayer.make(duration.toDouble(), text, fontSize.toDouble(), font.fontFamily, font.fontStyle))
    }

    actual var fillColor: Color get() = delegate.fillColor().asComposeColor()
        set(value) { delegate.setFillColor(value.asPAGColor()) }
    actual var font: PAGFont get() = delegate.font().let { PAGFont(it.fontFamily, it.fontStyle) }
        set(value) { delegate.setFont(PlatformPAGFont.create(value.fontFamily, value.fontStyle)) }
    actual var fontSize: Float get() = delegate.fontSize().toFloat()
        set(value) { delegate.setFontSize(value.toDouble()) }
    actual var strokeColor: Color get() = delegate.strokeColor().asComposeColor()
        set(value) { delegate.setStrokeColor(value.asPAGColor()) }
    actual var text: String get() = delegate.text()
        set(value) { delegate.setText(value) }
    actual fun reset() = delegate.reset()
}