package love.yinlin.platform

import kotlinx.browser.window
import love.yinlin.ui.component.screen.DialogProgressState

actual val osPlatform: Platform = Platform.WebWasm

actual fun osOpenUrl(url: String) {
	window.open(url, "_blank")
}

actual suspend fun osDownloadImage(url: String, state: DialogProgressState) = osOpenUrl(url)