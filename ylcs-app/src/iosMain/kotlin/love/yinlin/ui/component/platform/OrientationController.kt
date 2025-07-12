package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO:
@Composable
actual fun rememberOrientationController(): OrientationController {
    return remember { object : OrientationController {
        override var orientation: Orientation = PORTRAIT

        override fun rotate() {}
    } }
}