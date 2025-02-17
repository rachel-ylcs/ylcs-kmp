package love.yinlin.platform

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Stable
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.streams.asByteWriteChannel
import love.yinlin.data.MimeType
import love.yinlin.extension.fileSizeString
import love.yinlin.ui.component.screen.DialogProgressState

@Stable
actual object OS {
	@Stable
	actual val platform: Platform = Platform.Android

	actual fun openURL(url: String) {
		try {
			val uri = Uri.parse(url)
			val intent = Intent(Intent.ACTION_VIEW, uri)
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			appNative.context.startActivity(intent)
		}
		catch (_: Exception) { }
	}

	actual suspend fun downloadImage(url: String, state: DialogProgressState) {
		state.isOpen = true
		app.fileClient.safeCall { client ->
			client.prepareGet(url).execute { response ->
				val totalSize = response.bodyLength()
				Coroutines.main { state.total = totalSize.fileSizeString }
				val readChannel = response.bodyAsChannel()

				val filename = url.substringAfterLast('/').substringBefore('?')
				val values = ContentValues()
				values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
				values.put(MediaStore.Images.Media.MIME_TYPE, MimeType.IMAGE)
				values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
				val resolver = appNative.context.contentResolver
				val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
				val outputStream = resolver.openOutputStream(uri)!!
				val writeChannel = outputStream.asByteWriteChannel()

				try {
					Coroutines.io {
						transfer(readChannel, writeChannel, { state.isCancel }) {
							Coroutines.main {
								state.progress = if (totalSize != 0L) it.toFloat() / totalSize else 0f
								state.current = it.fileSizeString
							}
						}
					}
				}
				finally {
					writeChannel.flushAndClose()
					outputStream.close()
				}
			}
		}
		state.isOpen = false
	}
}