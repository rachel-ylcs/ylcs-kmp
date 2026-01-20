package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asComposeMatrix
import love.yinlin.compose.graphics.asSkiaMatrix33
import org.jetbrains.skia.Matrix33

actual open class PAGLayer(private val delegate: PlatformPAGLayer) {
    actual val layerType: PAGLayerType get() = PAGLayerType.entries[delegate.layerType]
    actual val layerName: String get() = delegate.layerName
    actual var matrix: Matrix get() = Matrix33(*delegate.matrix).asComposeMatrix()
        set(value) { delegate.matrix = value.asSkiaMatrix33().mat }
    actual fun resetMatrix() = delegate.resetMatrix()
    actual val totalMatrix: Matrix get() = Matrix33(*delegate.totalMatrix).asComposeMatrix()
    actual var visible: Boolean by delegate::visible
    actual val editableIndex: Int by delegate::editableIndex
    actual fun localTimeToGlobal(time: Long): Long = delegate.localTimeToGlobal(time)
    actual fun globalToLocalTime(time: Long): Long = delegate.globalToLocalTime(time)
    actual val duration: Long by delegate::duration
    actual val frameRate: Float by delegate::frameRate
    actual var startTime: Long by delegate::startTime
    actual var currentTime: Long by delegate::currentTime
    actual var progress: Double by delegate::progress
    actual val bounds: Rect get() = delegate.bounds.let { Rect(it[0], it[1], it[2], it[3]) }
    actual var excludedFromTimeline: Boolean by delegate::excludedFromTimeline
    actual var alpha: Float by delegate::alpha
}