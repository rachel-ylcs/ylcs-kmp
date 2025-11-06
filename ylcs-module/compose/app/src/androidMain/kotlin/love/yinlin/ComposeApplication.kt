package love.yinlin

import android.app.Application

abstract class ComposeApplication : Application() {
    abstract val instance: PlatformApplication<*>

    final override fun onCreate() {
        super.onCreate()
        instance.openService()
    }

    override fun onTerminate() {
        instance.closeService()
        super.onTerminate()
    }
}