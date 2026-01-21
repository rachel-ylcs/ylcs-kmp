@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asCGAffineTransform
import love.yinlin.compose.graphics.asComposeMatrix
import love.yinlin.compose.graphics.asComposeRect

@Stable
actual open class PAGLayer(internal open val delegate: PlatformPAGLayer) {
    actual val layerType: PAGLayerType get() = PAGLayerType.entries[delegate.layerType().ordinal]
    actual val layerName: String get() = delegate.layerName()!!
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asCGAffineTransform()) }
    actual fun resetMatrix() { delegate.resetMatrix() }
    actual val totalMatrix: Matrix get() = delegate.getTotalMatrix().asComposeMatrix()
    actual var visible: Boolean get() = delegate.visible()
        set(value) { delegate.setVisible(value) }
    actual val editableIndex: Int get() = delegate.editableIndex().toInt()
    actual fun localTimeToGlobal(time: Long): Long = delegate.localTimeToGlobal(time)
    actual fun globalToLocalTime(time: Long): Long = delegate.globalToLocalTime(time)
    actual val duration: Long get() = delegate.duration()
    actual val frameRate: Float get() = delegate.frameRate()
    actual var startTime: Long get() = delegate.startTime()
        set(value) { delegate.setStartTime(value) }
    actual var currentTime: Long get() = delegate.currentTime()
        set(value) { delegate.setCurrentTime(value) }
    actual var progress: Double get() = delegate.getProgress()
        set(value) { delegate.setProgress(value) }
    actual val bounds: Rect get() = delegate.getBounds().asComposeRect()
    actual var excludedFromTimeline: Boolean get() = delegate.excludedFromTimeline()
        set(value) { delegate.setExcludedFromTimeline(value) }
    actual var alpha: Float get() = delegate.alpha()
        set(value) { delegate.setAlpha(value) }
}