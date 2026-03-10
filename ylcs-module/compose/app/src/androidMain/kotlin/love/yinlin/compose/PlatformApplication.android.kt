package love.yinlin.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.extension.BaseLazyReference
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.AndroidContext
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.uri.Uri
import love.yinlin.uri.toAndroidUri

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    val delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    open fun BeginContent(activity: ComposeActivity) {}

    open fun onIntent(intent: Intent) {}

    actual fun openUri(uri: Uri): Boolean = catchingDefault(false) {
        val intent = Intent(Intent.ACTION_VIEW, uri.toAndroidUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        delegate.startActivity(intent)
        true
    }

    actual fun copyText(text: String): Boolean = catchingDefault(false) {
        val clipboard = context.application.getSystemService(AndroidContext.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
        true
    }
}