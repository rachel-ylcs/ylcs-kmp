package love.yinlin.platform

import kotlinx.browser.window

class AppContext : IAppContext() {
	override val screenWidth: Int = window.innerWidth
	override val screenHeight: Int = window.innerHeight
	override val fontScale: Float = 1f
	override val kv: KV = KV()
}

val appNative: AppContext get() = app as AppContext