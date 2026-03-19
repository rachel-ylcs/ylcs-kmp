package love.yinlin.foundation

import android.content.ContentResolver
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry

actual interface PlatformContextProvider {
    actual val raw: PlatformContext
    val contentResolver: ContentResolver
    val activity: ComponentActivity?
    val activityResultRegistry: ActivityResultRegistry?
}