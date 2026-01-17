package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix

@Stable
actual class PAGSourceScope actual constructor(private val layer: PAGSourceFile) {
    actual val width: Int
        get() = TODO("Not yet implemented")
    actual val height: Int
        get() = TODO("Not yet implemented")
    actual var matrix: Matrix
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun resetMatrix() {
    }

    actual var visible: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun localTimeToGlobal(time: Long): Long {
        TODO("Not yet implemented")
    }

    actual fun globalToLocalTime(time: Long): Long {
        TODO("Not yet implemented")
    }

    actual var duration: Long
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val frameRate: Float
        get() = TODO("Not yet implemented")
    actual var startTime: Long
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var currentTime: Long
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var progress: Double
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val bounds: Rect
        get() = TODO("Not yet implemented")
    actual var alpha: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val editableIndex: Int
        get() = TODO("Not yet implemented")
    actual val numChildren: Int
        get() = TODO("Not yet implemented")
    actual val numTexts: Int
        get() = TODO("Not yet implemented")
    actual val numImages: Int
        get() = TODO("Not yet implemented")
    actual val numVideos: Int
        get() = TODO("Not yet implemented")
    actual val audioBytes: ByteArray?
        get() = TODO("Not yet implemented")
    actual val audioStartTime: Long
        get() = TODO("Not yet implemented")
}