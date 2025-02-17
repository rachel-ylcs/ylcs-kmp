package love.yinlin;

import android.app.Application
import love.yinlin.platform.AppContext
import love.yinlin.platform.appContext

class RachelApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		appContext = AppContext(this).initialize()
	}
}