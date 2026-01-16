@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import cocoapods.libpag.*
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.compose.graphics.asComposeRect
import love.yinlin.extension.toByteArray

@Stable
actual class PAGSourceScope actual constructor(private val layer: PAGSourceFile) {
    actual val width: Int get() = layer.width().toInt()
    actual val height: Int get() = layer.height().toInt()

    actual var matrix: Matrix get() = layer.matrix()
        set(value) = { layer.setMatrix(value) }

    actual fun resetMatrix() { layer.resetMatrix() }

    actual var visible: Boolean get() = layer.visible()
        set(value) { layer.setVisible(value) }

    actual fun localTimeToGlobal(time: Long): Long = layer.localTimeToGlobal(time)

    actual fun globalToLocalTime(time: Long): Long = layer.globalToLocalTime(time)

    actual var duration: Long get() = layer.duration()
        set(value) { layer.setDuration(value) }

    actual val frameRate: Float get() = layer.frameRate()

    actual var startTime: Long get() = layer.startTime()
        set(value) { layer.setStartTime(value) }

    actual var currentTime: Long get() = layer.currentTime()
        set(value) { layer.setCurrentTime(value) }

    actual var progress: Double get() = layer.getProgress()
        set(value) { layer.setProgress(value) }

    actual val bounds: Rect get() = layer.getBounds().asComposeRect()

    actual var alpha: Float get() = layer.alpha()
        set(value) { layer.setAlpha(value) }


    actual val editableIndex: Int get() = layer.editableIndex().toInt()

    actual val numChildren: Int get() = layer.numChildren().toInt()

    actual val numTexts: Int get() = layer.numTexts()

    actual val numImages: Int get() = layer.numImages()

    actual val numVideos: Int get() = layer.numVideos()

    actual val audioBytes: ByteArray? get() = layer.audioBytes()?.toByteArray()

    actual val audioStartTime: Long get() = layer.audioStartTime()
}