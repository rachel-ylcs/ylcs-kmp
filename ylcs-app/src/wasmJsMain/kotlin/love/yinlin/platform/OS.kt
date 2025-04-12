package love.yinlin.platform

import kotlinx.browser.window
import kotlinx.io.files.Path

actual val osPlatform: Platform = Platform.WebWasm

actual fun osNetOpenUrl(url: String) {
	window.open(url, "_blank")
}

actual val osStorageDataPath: Path get() = error("wasmJs not supported")

actual val osStorageCachePath: Path get() = error("wasmJs not supported")