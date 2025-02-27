package love.yinlin;

import android.app.Application
import love.yinlin.platform.AppContext
import love.yinlin.platform.app

class RachelApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		app = AppContext(this).initialize()
	}
}