package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix

@Stable
expect class PAGSourceScope(layer: PAGSourceLayer) {
    val width: Int
    val height: Int
    var matrix: Matrix
    fun resetMatrix()
    var visible: Boolean
    fun localTimeToGlobal(time: Long): Long
    fun globalToLocalTime(time: Long): Long
    var duration: Long
    val frameRate: Float
    var startTime: Long
    var currentTime: Long
    var progress: Double
    val bounds: Rect
    var alpha: Float
}