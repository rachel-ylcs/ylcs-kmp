@file:JvmName("OSAndroid")
package love.yinlin.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.github.panpf.sketch.SingletonSketch
import kotlinx.io.files.Path
import androidx.core.net.toUri
import love.yinlin.common.Uri
import love.yinlin.common.toAndroidUri
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean = catchingDefault(false) {
	val intent = Intent(Intent.ACTION_VIEW, uri.toAndroidUri())
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	appNative.context.startActivity(intent)
	true
}

actual fun osApplicationCopyText(text: String): Boolean = catchingDefault(false) {
	val clipboard = appNative.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
	val clip = ClipData.newPlainText("", text)
	clipboard.setPrimaryClip(clip)
	true
}

actual fun osNetOpenUrl(url: String) = catching {
	val uri = url.toUri()
	val intent = Intent(Intent.ACTION_VIEW, uri)
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	appNative.context.startActivity(intent)
}

actual val osStorageDataPath: Path get() = Path(appNative.context.filesDir.absolutePath)

actual val osStorageCachePath: Path get() = Path(appNative.context.cacheDir.absolutePath)

actual val osStorageCacheSize: Long get() {
	val sketch = SingletonSketch.get(appNative.context)
	return sketch.downloadCache.size + sketch.resultCache.size
}

actual fun osStorageClearCache() {
	val sketch = SingletonSketch.get(appNative.context)
	sketch.downloadCache.clear()
	sketch.resultCache.clear()
}