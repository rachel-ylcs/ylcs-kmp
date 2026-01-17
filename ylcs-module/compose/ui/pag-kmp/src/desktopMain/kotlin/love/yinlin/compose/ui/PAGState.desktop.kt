package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

@Stable
actual class PAGState actual constructor(
    initComposition: PAGSourceComposition?,
    initProgress: Double,
    listener: PAGAnimationListener?,
) {
    actual var composition: PAGSourceComposition?
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var progress: Double
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun freeCache() {
    }

    actual fun makeSnapshot(): ImageBitmap? {
        TODO("Not yet implemented")
    }
}