package love.yinlin.platform

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.github.panpf.sketch.SingletonSketch
import io.ktor.utils.io.streams.*
import love.yinlin.data.MimeType
import love.yinlin.extension.fileSizeString
import love.yinlin.ui.component.screen.DialogProgressState

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

actual suspend fun osNetDownloadImage(url: String, state: DialogProgressState) {
	try {
		val filename = url.substringAfterLast('/').substringBefore('?')
		val values = ContentValues()
		values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
		values.put(MediaStore.Images.Media.MIME_TYPE, MimeType.IMAGE)
		values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
		val resolver = appNative.context.contentResolver
		val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

		state.open()
		app.fileClient.safeDownload(
			url = url,
			isCancel = { !state.isOpen },
			onStart = { state.total = it.fileSizeString },
			onTick = { current, total ->
				state.current = current.fileSizeString
				if (total != 0L) state.progress = current / total.toFloat()
			},
			onWriteBegin = { @SuppressLint("Recycle") resolver.openOutputStream(uri) },
			onWriteChannel = { it.asByteWriteChannel() },
			onWriteEnd = { it.close() }
		)
		state.hide()
	}
	catch (e: Exception) {
		e.printStackTrace()
	}
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