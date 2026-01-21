package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asComposeMatrix
import love.yinlin.compose.graphics.asSkiaMatrix33
import org.jetbrains.skia.Matrix33

@Stable
actual open class PAGLayer(internal open val delegate: PlatformPAGLayer) {
    actual val layerType: PAGLayerType get() = PAGLayerType.entries[delegate.layerType]
    actual val layerName: String get() = delegate.layerName
    actual var matrix: Matrix get() = Matrix33(*delegate.matrix).asComposeMatrix()
        set(value) { delegate.matrix = value.asSkiaMatrix33().mat }
    actual fun resetMatrix() = delegate.resetMatrix()
    actual val totalMatrix: Matrix get() = Matrix33(*delegate.totalMatrix).asComposeMatrix()
    actual var visible: Boolean get() = delegate.visible
        set(value) { delegate.visible = value }
    actual val editableIndex: Int get() = delegate.editableIndex
    actual fun localTimeToGlobal(time: Long): Long = delegate.localTimeToGlobal(time)
    actual fun globalToLocalTime(time: Long): Long = delegate.globalToLocalTime(time)
    actual val duration: Long get() = delegate.duration
    actual val frameRate: Float get() = delegate.frameRate
    actual var startTime: Long get() = delegate.startTime
        set(value) { delegate.startTime = value }
    actual var currentTime: Long get() = delegate.currentTime
        set(value) { delegate.currentTime = value }
    actual var progress: Double get() = delegate.progress
        set(value) { delegate.progress = value }
    actual val bounds: Rect get() = delegate.bounds.let { Rect(it[0], it[1], it[2], it[3]) }
    actual var excludedFromTimeline: Boolean get() = delegate.excludedFromTimeline
        set(value) { delegate.excludedFromTimeline = value }
    actual var alpha: Float get() = delegate.alpha
        set(value) { delegate.alpha = value }
}