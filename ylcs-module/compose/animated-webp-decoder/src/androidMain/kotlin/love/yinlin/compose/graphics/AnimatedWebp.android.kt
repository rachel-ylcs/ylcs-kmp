package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catchingNull

@Stable
actual class AnimatedWebp internal constructor(private val handle: Long) {
    actual val width: Int = nativeAnimatedWebpGetWidth(handle)
    actual val height: Int = nativeAnimatedWebpGetHeight(handle)
    actual val frameCount: Int = 0

    actual fun DrawScope.drawFrame(index: Int, dst: Rect) {

    }

    actual fun encode(format: ImageFormat, quality: ImageQuality): ByteArray? = null

    actual companion object {
        init {
            System.loadLibrary("webp_jni")
        }

        actual fun decode(data: ByteArray): AnimatedWebp? = catchingNull {
            null
        }
    }
}