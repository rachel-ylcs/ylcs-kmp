package love.yinlin.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.extension.BaseLazyReference
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.Uri
import love.yinlin.uri.toAndroidUri

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A>(self, context) {
    @Composable
    open fun BeginContent(activity: ComposeActivity) {}

    open fun onIntent(intent: Intent) {}

    final override val contentResolver: ContentResolver get() = raw.contentResolver

    final override var activity: ComponentActivity? = null
        internal set

    final override val activityResultRegistry: ActivityResultRegistry? get() = activity?.activityResultRegistry

    actual fun backHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        raw.startActivity(intent)
    }

    actual fun openUri(uri: Uri): Boolean = catchingDefault(false) {
        val intent = Intent(Intent.ACTION_VIEW, uri.toAndroidUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        raw.startActivity(intent)
        true
    }

    actual fun copyText(text: String): Boolean = catchingDefault(false) {
        val clipboard = raw.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
        true
    }
}