package love.yinlin.foundation

import android.content.pm.ActivityInfo

actual class OrientationController actual constructor(context: Context) {
    private val activity = context.activity

    actual var orientation: Orientation
        get() = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) Orientation.Landscape else Orientation.Portrait
        set(value) {
            activity.requestedOrientation = if (value == Orientation.Landscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

    actual fun rotate() {
        activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    actual fun restore() {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}