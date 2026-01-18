package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix

expect class PAGImage {
    val width: Int
    val height: Int
    var scaleMode: PAGScaleMode
    var matrix: Matrix

    fun close()
}