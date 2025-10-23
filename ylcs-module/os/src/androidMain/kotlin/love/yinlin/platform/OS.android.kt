package love.yinlin.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import kotlinx.io.files.Path
import love.yinlin.appContext
import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.toAndroidUri
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean = catchingDefault(false) {
	val intent = Intent(Intent.ACTION_VIEW, uri.toAndroidUri())
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	appContext.context.startActivity(intent)
	true
}

actual fun osApplicationCopyText(text: String): Boolean = catchingDefault(false) {
	val clipboard = appContext.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
	val clip = ClipData.newPlainText("", text)
	clipboard.setPrimaryClip(clip)
	true
}

actual fun osNetOpenUrl(uri: Uri) = catching {
	val intent = Intent(Intent.ACTION_VIEW, uri.toAndroidUri())
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	appContext.context.startActivity(intent)
}

actual val osStorageDataPath: Path get() = Path(appContext.context.filesDir.absolutePath)

actual val osStorageCachePath: Path get() = Path(appContext.context.cacheDir.absolutePath)

actual val osStorageCacheSize: Long get() {
	return 0L
	// TODO:
//	val sketch = SingletonSketch.get(appNative.context)
//	return sketch.downloadCache.size + sketch.resultCache.size
}

actual fun osStorageClearCache() {
//	val sketch = SingletonSketch.get(appNative.context)
//	sketch.downloadCache.clear()
//	sketch.resultCache.clear()
}