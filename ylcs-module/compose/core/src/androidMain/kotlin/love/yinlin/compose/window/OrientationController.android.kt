package love.yinlin.compose.window

import android.content.pm.ActivityInfo
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import love.yinlin.foundation.PlatformContextProvider

@Stable
actual class OrientationController {
    actual fun getOrientation(context: PlatformContextProvider): Orientation =
        if (context.activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) Orientation.Horizontal else Orientation.Vertical

    actual fun setOrientation(context: PlatformContextProvider, orientation: Orientation) {
        context.activity?.requestedOrientation = if (orientation == Orientation.Horizontal) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    actual fun rotate(context: PlatformContextProvider) {
        context.activity?.let { activity ->
            activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    actual fun store(context: PlatformContextProvider) {
        context.activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}