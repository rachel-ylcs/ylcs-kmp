package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberOrientationController(): OrientationController {
    return remember { object : OrientationController {
        override var orientation: Orientation = Orientation.LANDSCAPE
        override fun rotate() {}
    } }
}