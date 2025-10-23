package love.yinlin

import android.app.Application
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app

class RachelApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		app1 = AppContext1(this)
		ActualAppContext(this).apply {
			app = this
			initialize()
		}
	}
}