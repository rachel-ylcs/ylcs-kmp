package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import love.yinlin.extension.catchingNull

@Stable
actual class AnimatedWebp internal constructor(
    actual val width: Int,
    actual val height: Int,
    actual val frameCount: Int,
) {
    actual suspend fun nextFrame() {

    }

    actual fun resetFrame() {

    }

    actual fun DrawScope.drawFrame(dst: Rect, src: Rect?) {

    }

    actual fun release() {

    }

    actual companion object {
        actual fun decode(data: ByteArray): AnimatedWebp? = catchingNull {
            null
        }
    }
}