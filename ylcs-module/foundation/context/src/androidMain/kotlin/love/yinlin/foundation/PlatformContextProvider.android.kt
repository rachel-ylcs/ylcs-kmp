package love.yinlin.foundation

import android.content.ContentResolver
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry

actual open class PlatformContextProvider actual constructor(actual val rawContext: PlatformContext) {
    val contentResolver: ContentResolver get() = rawContext.contentResolver
    var activity: ComponentActivity? = null
    val activityResultRegistry: ActivityResultRegistry? get() = activity?.activityResultRegistry
}