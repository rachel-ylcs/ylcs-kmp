package love.yinlin.compose

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
data class ActualOrientationController(val activity: Activity) : OrientationController {
    override var orientation: Orientation
        get() = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) Orientation.LANDSCAPE else Orientation.PORTRAIT
        set(value) {
            activity.requestedOrientation = if (value == Orientation.LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

    override fun rotate() {
        activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

@Composable
actual fun rememberOrientationController(): OrientationController {
    val activity = LocalActivity.current!!
    val controller = remember(activity) { ActualOrientationController(activity) }

    DisposableEffect(activity) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    return controller
}