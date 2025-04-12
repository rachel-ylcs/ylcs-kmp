package love.yinlin;

import android.app.Application
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app

class RachelApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		val context = ActualAppContext(this)
		app = context
		context.initialize()
	}
}