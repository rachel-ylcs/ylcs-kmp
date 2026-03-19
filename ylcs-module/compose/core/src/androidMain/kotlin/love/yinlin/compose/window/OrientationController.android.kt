package love.yinlin.compose.window

import android.content.pm.ActivityInfo
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContextProvider

@Stable
actual class OrientationController actual constructor(private val context: PlatformContextProvider) {
    actual var orientation: Orientation
        get() = if (context.activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) Orientation.Horizontal else Orientation.Vertical
        set(value) {
            context.activity?.requestedOrientation = if (value == Orientation.Horizontal) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

    actual fun rotate() {
        context.activity?.let { activity ->
            activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    actual fun restore() {
        context.activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}