package love.yinlin.compose.window

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContextProvider

@Stable
expect class OrientationController() {
    fun getOrientation(context: PlatformContextProvider): Orientation
    fun setOrientation(context: PlatformContextProvider, orientation: Orientation)
    fun rotate(context: PlatformContextProvider)
    fun store(context: PlatformContextProvider)
}