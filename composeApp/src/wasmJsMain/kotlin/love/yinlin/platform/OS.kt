package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.browser.window
import love.yinlin.ui.component.screen.DialogProgressState

@Stable
actual object OS {
	@Stable
	actual val platform: Platform = Platform.WebWasm

	actual fun openURL(url: String) {
		window.open(url, "_blank")
	}

	actual suspend fun downloadImage(url: String, state: DialogProgressState) = openURL(url)
}