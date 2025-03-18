package love.yinlin.common

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.LifecycleOwner
import com.forjrking.lubankt.Luban
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val WebpFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
	Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

suspend fun compressImage(
	lifecycle: LifecycleOwner,
	image: Uri,
	maxFileSize: Int = 150,
	quality: Int = 90
): File = withContext(Dispatchers.Default) {
	Luban.with(lifecycle).load(image)
		.concurrent(true)
		.rename { it.substringBeforeLast('.') + ".webp" }
		.useDownSample(true)
		.format(WebpFormat)
		.ignoreBy(maxFileSize.toLong())
		.quality(quality)
		.get()
}

suspend fun compressImages(
	lifecycle: LifecycleOwner,
	images: List<Uri>,
	maxFileSize: Int = 150,
	quality: Int = 90
): List<File> = withContext(Dispatchers.Default) {
	Luban.with(lifecycle).load(images)
		.concurrent(true)
		.rename { it.substringBeforeLast('.') + ".webp" }
		.useDownSample(true)
		.format(WebpFormat)
		.ignoreBy(maxFileSize.toLong())
		.quality(quality)
		.get()
}

suspend fun compressImage(
	lifecycle: LifecycleOwner,
	image: ImageBitmap,
	maxFileSize: Int = 150,
	quality: Int = 90
): File = withContext(Dispatchers.Default) {
	Luban.with(lifecycle).load(image.asAndroidBitmap())
		.concurrent(true)
		.rename { it.substringBeforeLast('.') + ".webp" }
		.useDownSample(true)
		.format(WebpFormat)
		.ignoreBy(maxFileSize.toLong())
		.quality(quality)
		.get()
}