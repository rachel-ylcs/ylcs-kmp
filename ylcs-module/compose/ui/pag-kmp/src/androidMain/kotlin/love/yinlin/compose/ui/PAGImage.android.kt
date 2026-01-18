package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asAndroidMatrix
import love.yinlin.compose.graphics.asComposeMatrix

actual class PAGImage(private val delegate: PlatformPAGImage) : AutoCloseable {
    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode()]
        set(value) { delegate.setScaleMode(value.ordinal) }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asAndroidMatrix()) }

    actual override fun close() = delegate.release()
}