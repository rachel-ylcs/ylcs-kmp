package love.yinlin.platform

import androidx.compose.runtime.Stable
import io.github.vinceglb.filekit.core.FileKit
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.util.cio.writeChannel
import love.yinlin.extension.fileSizeString
import love.yinlin.ui.component.screen.DialogProgressState
import java.awt.Desktop
import java.io.File
import java.net.URI

@Stable
actual object OS {
	@Stable
	actual val platform: Platform = System.getProperty("os.name").let {
		when {
			it.startsWith("Windows") -> Platform.Windows
			it.startsWith("Mac") -> Platform.MacOS
			else -> Platform.Linux
		}
	}

	actual fun openURL(url: String) {
		try {
			val desktop = Desktop.getDesktop()
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(URI(url))
			}
		}
		catch (_: Exception) { }
	}

	actual suspend fun downloadImage(url: String, state: DialogProgressState) {
		val filename = url.substringAfterLast('/').substringBefore('?')
		val outFilename = FileKit.saveFile(bytes = null, baseName = filename, extension = "*")
		if (outFilename != null) {
			state.isOpen = true
			app.fileClient.safeCall { client ->
				client.prepareGet(url).execute { response ->
					val totalSize = response.bodyLength()
					Coroutines.main { state.total = totalSize.fileSizeString }
					val readChannel = response.bodyAsChannel()
					val writeChannel = outFilename.file.writeChannel()

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
					}
				}
			}
			state.isOpen = false
		}
	}
}