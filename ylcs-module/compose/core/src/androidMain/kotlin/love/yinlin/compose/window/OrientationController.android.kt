package love.yinlin.compose.window

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberOrientationController(): OrientationController {
    val activity = LocalContext.current as? Activity

    DisposableEffect(activity) {
        onDispose {
            if (activity != null) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    return remember(activity) {
        object : OrientationController {
            override var orientation: Orientation
                get() = if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) Orientation.Horizontal else Orientation.Vertical
                set(value) {
                    activity?.requestedOrientation = if (value == Orientation.Horizontal) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }

            override fun rotate() {
                activity?.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }
}