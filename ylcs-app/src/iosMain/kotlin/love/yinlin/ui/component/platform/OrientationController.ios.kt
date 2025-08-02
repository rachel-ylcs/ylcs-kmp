package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS待实现屏幕旋转
@Composable
actual fun rememberOrientationController(): OrientationController {
    return remember { object : OrientationController {
        override var orientation: Orientation = Orientation.PORTRAIT

        override fun rotate() {}
    } }
}