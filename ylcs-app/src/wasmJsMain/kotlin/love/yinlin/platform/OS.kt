package love.yinlin.platform

import kotlinx.browser.window
import kotlinx.io.files.Path
import love.yinlin.common.Uri

actual val osPlatform: Platform = Platform.WebWasm

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean {
	osNetOpenUrl(uri.toString())
	return true
}

actual fun osApplicationCopyText(text: String): Boolean {
	window.navigator.clipboard.writeText(text)
	return true
}

actual fun osNetOpenUrl(url: String) {
	window.open(url, "_blank")
}

actual val osStorageDataPath: Path get() = unsupportedPlatform()

actual val osStorageCachePath: Path get() = unsupportedPlatform()