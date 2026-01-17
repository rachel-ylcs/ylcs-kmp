package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import org.khronos.webgl.toUByteArray

@Stable
actual class PAGSourceScope actual constructor(private val layer: PAGSourceFile) {
    actual val width: Int get() = layer.width()

    actual val height: Int get() = layer.height()

    actual var matrix: Matrix get() = layer.matrix().asComposeMatrix()
        set(value) { layer.setMatrix(value.asPAGMatrix()) }

    actual fun resetMatrix() = layer.resetMatrix()

    actual var visible: Boolean get() = layer.visible()
        set(value) { layer.setVisible(value) }

    actual fun localTimeToGlobal(time: Long): Long = layer.localTimeToGlobal(time)

    actual fun globalToLocalTime(time: Long): Long = layer.globalToLocalTime(time)

    actual var duration: Long get() = layer.duration()
        set(_) { }

    actual val frameRate: Float get() = layer.frameRate()

    actual var startTime: Long get() = layer.startTime()
        set(value) { layer.setStartTime(value) }

    actual var currentTime: Long get() = layer.currentTime()
        set(value) { layer.setCurrentTime(value) }

    actual var progress: Double get() = layer.getProgress()
        set(value) { layer.setProgress(value) }

    actual val bounds: Rect get() = layer.getBounds().asComposeRect()

    actual var alpha: Float get() = layer.alpha().toFloat()
        set(value) { layer.setAlpha(value.toDouble()) }

    actual val editableIndex: Int get() = layer.editableIndex()

    actual val numChildren: Int get() = layer.numChildren()

    actual val numTexts: Int = 0

    actual val numImages: Int = 0

    actual val numVideos: Int = 0

    @OptIn(ExperimentalUnsignedTypes::class)
    actual val audioBytes: ByteArray? get() = layer.audioBytes()?.toUByteArray()?.toByteArray()
    
    actual val audioStartTime: Long get() = layer.audioStartTime()
}