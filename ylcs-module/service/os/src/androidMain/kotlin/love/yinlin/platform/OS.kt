package love.yinlin.platform

import android.content.Context
import android.content.Intent
import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.toAndroidUri

internal data object OSUtil {
    fun openUri(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri.toAndroidUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}