package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix

actual open class PAGLayer(private val delegate: PlatformPAGLayer) {
    actual val layerType: PAGLayerType get() = PAGLayerType.entries[delegate.layerType()]
    actual val layerName: String get() = delegate.layerName()
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asPAGMatrix()) }
    actual fun resetMatrix() = delegate.resetMatrix()
    actual val totalMatrix: Matrix get() = delegate.getTotalMatrix().asComposeMatrix()
    actual var visible: Boolean get() = delegate.visible()
        set(value) { delegate.setVisible(value) }
    actual val editableIndex: Int get() = delegate.editableIndex()
    actual fun localTimeToGlobal(time: Long): Long = delegate.localTimeToGlobal(time.toDouble()).toLong()
    actual fun globalToLocalTime(time: Long): Long = delegate.globalToLocalTime(time.toDouble()).toLong()
    actual val duration: Long get() = delegate.duration().toLong()
    actual val frameRate: Float get() = delegate.frameRate()
    actual var startTime: Long get() = delegate.startTime().toLong()
        set(value) { delegate.setStartTime(value.toDouble()) }
    actual var currentTime: Long get() = delegate.currentTime().toLong()
        set(value) { delegate.setCurrentTime(value.toDouble()) }
    actual var progress: Double get() = delegate.getProgress()
        set(value) { delegate.setProgress(value) }
    actual val bounds: Rect get() = delegate.getBounds().asComposeRect()
    actual var excludedFromTimeline: Boolean get() = delegate.excludedFromTimeline()
        set(value) { delegate.setExcludedFromTimeline(value) }
    actual var alpha: Float get() = delegate.alpha().toFloat()
        set(value) { delegate.setAlpha(value.toDouble()) }
}