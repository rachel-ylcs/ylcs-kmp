package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
expect class PAGTextLayer : PAGLayer {
    companion object {
        fun make(duration: Long, text: String, fontSize: Float = 24f, font: PAGFont = PAGFont()): PAGTextLayer
    }

    var fillColor: Color
    var font: PAGFont
    var fontSize: Float
    var strokeColor: Color
    var text: String
    fun reset()
}