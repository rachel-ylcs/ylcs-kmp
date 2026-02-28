package love.yinlin.compose

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class ComposeApplication : Application() {
    abstract val instance: PlatformApplication<*>

    private val applicationScope = MainScope()

    final override fun onCreate() {
        super.onCreate()
        instance.openService(scope = applicationScope, later = false, immediate = false)
    }

    override fun onTerminate() {
        instance.closeService(before = false, immediate = false)
        applicationScope.cancel()
        super.onTerminate()
    }
}