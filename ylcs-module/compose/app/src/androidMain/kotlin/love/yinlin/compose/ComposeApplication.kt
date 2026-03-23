package love.yinlin.compose

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class ComposeApplication : Application() {
    abstract fun buildInstance(): PlatformApplication<*>

    internal lateinit var instance: PlatformApplication<*>
        private set

    private val applicationScope = MainScope()

    final override fun onCreate() {
        super.onCreate()
        if (!::instance.isInitialized) {
            instance = buildInstance()
            instance.initApplicationService(scope = applicationScope)
        }
    }

    final override fun onTerminate() {
        if (::instance.isInitialized) instance.destroyService()
        applicationScope.cancel()
        super.onTerminate()
    }
}