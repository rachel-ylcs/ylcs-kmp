package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
enum class Orientation {
    PORTRAIT, LANDSCAPE
}

@Stable
interface OrientationController {
    var orientation: Orientation
    fun rotate()
}

@Composable
expect fun rememberOrientationController(): OrientationController