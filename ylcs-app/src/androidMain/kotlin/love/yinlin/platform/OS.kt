@file:JvmName("OSAndroid")
package love.yinlin.platform

import android.content.Intent
import com.github.panpf.sketch.SingletonSketch
import kotlinx.io.files.Path
import androidx.core.net.toUri
import love.yinlin.common.Uri
import love.yinlin.common.toAndroidUri

actual val osPlatform: Platform = Platform.Android

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean = try {
	val intent = Intent(Intent.ACTION_VIEW, uri.toAndroidUri())
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	appNative.context.startActivity(intent)
	true
}
catch (_: Throwable) { false }

actual fun osNetOpenUrl(url: String) {
	try {
		val uri = url.toUri()
		val intent = Intent(Intent.ACTION_VIEW, uri)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		appNative.context.startActivity(intent)
	}
	catch (_: Exception) { }
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