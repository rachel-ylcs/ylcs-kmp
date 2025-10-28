package love.yinlin

import love.yinlin.compose.RachelApplication

class MainApplication : RachelApplication() {
	override fun onCreate() {
		super.onCreate()
		service.init(this)
	}
}