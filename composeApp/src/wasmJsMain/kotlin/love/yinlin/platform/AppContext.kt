package love.yinlin.platform

import kotlinx.browser.window

class AppContext : AppContextBase() {
	override val screenWidth: Int = window.innerWidth
	override val screenHeight: Int = window.innerHeight
	override val fontScale: Float = 1f
	override val kv: KV = KV()

	override fun initialize(): AppContextBase {
		super.initialize()
		return this
	}
}

val appNative: AppContext get() = app as AppContext