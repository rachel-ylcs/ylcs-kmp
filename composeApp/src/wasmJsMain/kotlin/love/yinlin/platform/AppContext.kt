package love.yinlin.platform

import kotlinx.browser.window

class ActualAppContext : AppContext() {
	override val screenWidth: Int = window.innerWidth
	override val screenHeight: Int = window.innerHeight
	override val fontScale: Float = 1f
	override val kv: KV = KV()

	override fun initialize(): AppContext {
		super.initialize()
		return this
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext