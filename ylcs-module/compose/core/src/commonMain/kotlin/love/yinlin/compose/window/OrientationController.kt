package love.yinlin.compose.window

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface OrientationController {
    companion object {
        internal val None = object : OrientationController {
            override var orientation: Orientation get() = Orientation.Vertical
                set(value) { }
            override fun rotate() { }
        }
    }

    var orientation: Orientation
    fun rotate()
}

@Composable
expect fun rememberOrientationController(): OrientationController