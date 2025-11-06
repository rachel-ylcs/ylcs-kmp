package love.yinlin

import android.app.Application

abstract class ComposeApplication : Application() {
    abstract val instance: PlatformApplication<*>

    final override fun onCreate() {
        super.onCreate()
        instance.openService(later = false, immediate = false)
    }

    override fun onTerminate() {
        instance.closeService(before = false, immediate = false)
        super.onTerminate()
    }
}