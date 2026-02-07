package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

@Stable
actual open class PAGState {
    internal var stateProgress: Double by mutableDoubleStateOf(0.0)
    internal var flushFlag: Long by mutableLongStateOf(0L)
    internal var player: PAGPlayer? = null

    actual var progress: Double get() = stateProgress
        set(value) {
            player?.let {
                it.progress = value
                it.flush()
            }
        }

    actual fun flush() { ++flushFlag }
    actual fun freeCache() { player?.surface?.freeCache() }

    actual fun makeSnapshot(): ImageBitmap? = player?.surface?.makeSnapshot()

    actual open fun onAnimationStart() { }
    actual open fun onAnimationEnd() { }
    actual open fun onAnimationCancel() { }
    actual open fun onAnimationRepeat() { }
}