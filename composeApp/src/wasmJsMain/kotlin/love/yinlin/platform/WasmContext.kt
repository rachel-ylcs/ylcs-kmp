package love.yinlin.platform

import kotlinx.browser.window

class WasmContext : AppContext() {
	override val screenWidth: Int = window.innerWidth
	override val screenHeight: Int = window.innerHeight
	override val fontScale: Float = 1f
}

actual val platform: Platform = Platform.WebWasm