package love.yinlin.compose

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import love.yinlin.extension.cleaning

abstract class ComposeApplication : Application() {
    abstract fun buildInstance(): PlatformApplication<*>

    internal lateinit var instance: PlatformApplication<*>
        private set

    private val applicationScope = MainScope()

    final override fun onCreate() {
        super.onCreate()
        if (!::instance.isInitialized) {
            instance = buildInstance()
            instance.initApplication(scope = applicationScope)
        }
    }

    final override fun onTerminate() {
        cleaning({
            if (::instance.isInitialized) instance.destroyPool()
        }) {
            cleaning(applicationScope::cancel) {
                super.onTerminate()
            }
        }
    }
}