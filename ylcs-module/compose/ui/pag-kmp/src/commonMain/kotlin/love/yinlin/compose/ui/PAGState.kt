package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

@Stable
expect class PAGState(
    initComposition: PAGSourceComposition? = null,
    initProgress: Double = 0.0,
    listener: PAGAnimationListener? = null,
) {
    var composition: PAGSourceComposition?
    var progress: Double

    fun freeCache()
    fun makeSnapshot(): ImageBitmap?
}