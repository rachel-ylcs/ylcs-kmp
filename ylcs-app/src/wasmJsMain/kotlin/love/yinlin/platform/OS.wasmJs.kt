package love.yinlin.platform

import kotlinx.browser.window
import kotlinx.io.files.Path
import love.yinlin.common.Uri

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean {
	osNetOpenUrl(uri.toString())
	return true
}

@OptIn(ExperimentalWasmJsInterop::class)
actual fun osApplicationCopyText(text: String): Boolean {
	window.navigator.clipboard.writeText(text)
	return true
}

fun osApplicationIsStandalone() = window.matchMedia("(display-mode: standalone)").matches

actual fun osNetOpenUrl(url: String) {
	window.open(url, "_blank")
}

actual val osStorageDataPath: Path get() = unsupportedPlatform()

actual val osStorageCachePath: Path get() = unsupportedPlatform()