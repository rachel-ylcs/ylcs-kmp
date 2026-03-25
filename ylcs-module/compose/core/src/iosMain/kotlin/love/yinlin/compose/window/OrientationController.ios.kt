package love.yinlin.compose.window

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContextProvider

@Stable
actual class OrientationController {
    actual fun getOrientation(context: PlatformContextProvider): Orientation = Orientation.Vertical
    actual fun setOrientation(context: PlatformContextProvider, orientation: Orientation) { }
    actual fun rotate(context: PlatformContextProvider) { }
    actual fun store(context: PlatformContextProvider) { }
}