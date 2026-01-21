package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix

@Stable
expect open class PAGLayer {
    val layerType: PAGLayerType
    val layerName: String
    var matrix: Matrix
    fun resetMatrix()
    val totalMatrix: Matrix
    var visible: Boolean
    val editableIndex: Int
    fun localTimeToGlobal(time: Long): Long
    fun globalToLocalTime(time: Long): Long
    val duration: Long
    val frameRate: Float
    var startTime: Long
    var currentTime: Long
    var progress: Double
    val bounds: Rect
    var excludedFromTimeline: Boolean
    var alpha: Float
}