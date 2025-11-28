package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import love.yinlin.extension.catchingNull

@Stable
actual class AnimatedWebp internal constructor(private val handle: Long) {
    actual val width: Int = nativeAnimatedWebpGetWidth(handle)
    actual val height: Int = nativeAnimatedWebpGetHeight(handle)
    actual val frameCount: Int = 2

    actual suspend fun nextFrame() {

    }

    actual fun resetFrame() {

    }

    actual fun DrawScope.drawFrame(dst: Rect, src: Rect?) {

    }

    actual fun release() {
        nativeAnimatedWebpRelease(handle)
    }

    actual companion object {
        init {
            System.loadLibrary("webp_jni")
        }

        actual fun decode(data: ByteArray): AnimatedWebp? = catchingNull {
            val handle = nativeAnimatedWebpCreate(data)
            return if (handle != 0L) AnimatedWebp(handle) else null
        }
    }
}