package love.yinlin.platform

import android.content.Intent
import android.net.Uri
import com.github.panpf.sketch.SingletonSketch

actual val osPlatform: Platform = Platform.Android

actual fun osNetOpenUrl(url: String) {
	try {
		val uri = Uri.parse(url)
		val intent = Intent(Intent.ACTION_VIEW, uri)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		appNative.context.startActivity(intent)
	}
	catch (_: Exception) { }
}

actual val osStorageCachePath: String get() = appNative.context.cacheDir.absolutePath

actual val osStorageCacheSize: Long get() {
	val sketch = SingletonSketch.get(appNative.context)
	return sketch.downloadCache.size + sketch.resultCache.size
}

actual fun osStorageClearCache() {
	val sketch = SingletonSketch.get(appNative.context)
	sketch.downloadCache.clear()
	sketch.resultCache.clear()
}