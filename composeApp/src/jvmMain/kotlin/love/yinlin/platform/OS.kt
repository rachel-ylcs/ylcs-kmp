package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.files.Path
import love.yinlin.extension.fileSizeString
import love.yinlin.ui.component.screen.DialogProgressState
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
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
		val saveFile = suspendCancellableCoroutine { continuation ->
			val filename = url.substringAfterLast('/').substringBefore('?')
			val fileDialog = FileDialog(null as? Frame?, "保存到", FileDialog.SAVE)
			fileDialog.isMultipleMode = false
			fileDialog.file = filename
			fileDialog.isVisible = true
			continuation.resume(fileDialog.files?.firstOrNull()) { _, _, _ -> fileDialog.dispose() }
		}

		if (saveFile != null) {
			state.open()
			app.fileClient.safeDownload(
				url = url,
				file = Path(saveFile.path),
				isCancel = { !state.isOpen },
				onStart = { state.total = it.fileSizeString }
			) { current, total ->
				state.current = current.fileSizeString
				if (total != 0L) state.progress = current / total.toFloat()
			}
			state.hide()
		}
	}
}