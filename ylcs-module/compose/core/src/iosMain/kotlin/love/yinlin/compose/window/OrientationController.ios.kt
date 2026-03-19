package love.yinlin.compose.window

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContextProvider

@Stable
actual class OrientationController actual constructor(context: PlatformContextProvider) {
    actual var orientation: Orientation
        get() = Orientation.Vertical
        set(_) {}
    actual fun rotate() { }
    actual fun restore() { }
}