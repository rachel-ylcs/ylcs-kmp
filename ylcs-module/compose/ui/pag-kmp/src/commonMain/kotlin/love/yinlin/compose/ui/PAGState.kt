package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

@Stable
expect open class PAGState() {
    var progress: Double

    fun flush()
    fun freeCache()
    fun makeSnapshot(): ImageBitmap?

    open fun onAnimationStart()
    open fun onAnimationEnd()
    open fun onAnimationCancel()
    open fun onAnimationRepeat()
}