package love.yinlin

import android.app.Application

class RachelApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		service.init(this)
	}
}