package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.toComposeRect
import love.yinlin.compose.graphics.asAndroidMatrix
import love.yinlin.compose.graphics.asComposeMatrix

actual open class PAGLayer(private val delegate: PlatformPAGLayer) {
    actual val layerType: PAGLayerType get() = PAGLayerType.entries[delegate.layerType()]
    actual val layerName: String get() = delegate.layerName()
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asAndroidMatrix()) }
    actual fun resetMatrix() = delegate.resetMatrix()
    actual val totalMatrix: Matrix get() = delegate.totalMatrix.asComposeMatrix()
    actual var visible: Boolean get() = delegate.visible()
        set(value) { delegate.setVisible(value) }
    actual val editableIndex: Int get() = delegate.editableIndex()
    actual fun localTimeToGlobal(time: Long): Long = delegate.localTimeToGlobal(time)
    actual fun globalToLocalTime(time: Long): Long = delegate.globalToLocalTime(time)
    actual val duration: Long get() = delegate.duration()
    actual val frameRate: Float get() = delegate.frameRate()
    actual var startTime: Long get() = delegate.startTime()
        set(value) { delegate.setStartTime(value) }
    actual var currentTime: Long get() = delegate.currentTime()
        set(value) { delegate.setCurrentTime(value) }
    actual var progress: Double get() = delegate.progress
        set(value) { delegate.setProgress(value) }
    actual val bounds: Rect get() = delegate.bounds.toComposeRect()
    actual var excludedFromTimeline: Boolean get() = delegate.excludedFromTimeline()
        set(value) { delegate.setExcludedFromTimeline(value) }
    actual var alpha: Float get() = delegate.alpha()
        set(value) { delegate.setAlpha(value) }
}