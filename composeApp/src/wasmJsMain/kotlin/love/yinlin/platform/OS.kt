package love.yinlin.platform

import kotlinx.browser.window

actual val osPlatform: Platform = Platform.WebWasm

actual fun osNetOpenUrl(url: String) {
	window.open(url, "_blank")
}

actual val osStorageCachePath: String get() = ""