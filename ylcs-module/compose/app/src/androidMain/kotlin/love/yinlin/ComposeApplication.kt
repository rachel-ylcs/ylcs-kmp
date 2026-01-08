package love.yinlin

import android.app.Application
import love.yinlin.platform.SharedLibs

abstract class ComposeApplication : Application() {
    abstract val instance: PlatformApplication<*>

    final override fun onCreate() {
        super.onCreate()
        SharedLibs.load()
        instance.openService(later = false, immediate = false)
    }

    override fun onTerminate() {
        instance.closeService(before = false, immediate = false)
        super.onTerminate()
    }
}