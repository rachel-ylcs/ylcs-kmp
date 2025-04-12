package love.yinlin.platform

import android.content.Intent
import com.github.panpf.sketch.SingletonSketch
import kotlinx.io.files.Path
import androidx.core.net.toUri

actual val osPlatform: Platform = Platform.Android

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