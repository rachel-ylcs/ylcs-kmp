package love.yinlin.platform

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

actual val osStorageCacheSize: Long get() {
	val sketch = SingletonSketch.get(appNative.context)
	return sketch.downloadCache.size + sketch.resultCache.size
}

actual fun osStorageClearCache() {
	val sketch = SingletonSketch.get(appNative.context)
	sketch.downloadCache.clear()
	sketch.resultCache.clear()
}

actual fun osImageCrop(bitmap: ImageBitmap, startX: Int, startY: Int, width: Int, height: Int): ImageBitmap {
	return Bitmap.createBitmap(bitmap.asAndroidBitmap(), startX, startY, width, height).asImageBitmap()
}