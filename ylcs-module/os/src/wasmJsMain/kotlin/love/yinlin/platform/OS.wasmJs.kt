package love.yinlin.platform

import kotlinx.browser.window
import kotlinx.io.files.Path
import love.yinlin.common.uri.Uri

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean {
	osNetOpenUrl(uri)
	return true
}

@OptIn(ExperimentalWasmJsInterop::class)
actual fun osApplicationCopyText(text: String): Boolean {
	window.navigator.clipboard.writeText(text)
	return true
}

fun osApplicationIsStandalone() = window.matchMedia("(display-mode: standalone)").matches

actual fun osNetOpenUrl(uri: Uri) {
	window.open(uri.toString(), "_blank")
}

actual val osStorageDataPath: Path get() = unsupportedPlatform()

actual val osStorageCachePath: Path get() = unsupportedPlatform()

actual val osStorageCacheSize: Long get() {
	// TODO:
	return 0L
}

actual fun osStorageClearCache() {

}