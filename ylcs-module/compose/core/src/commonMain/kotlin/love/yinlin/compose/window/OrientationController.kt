package love.yinlin.compose.window

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContextProvider

@Stable
expect class OrientationController(context: PlatformContextProvider) {
    var orientation: Orientation
    fun rotate()
    fun restore()
}