package love.yinlin.startup

import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory

class StartupExceptionHandler(
    pool: StartupPool,
    val crashKey: String,
    handler: ExceptionHandler
) : SyncStartup(pool) {
    class Factory(
        private val crashKey: String,
        private val handler: ExceptionHandler
    ) : SyncStartupFactory<StartupExceptionHandler>() {
        override val id: String = StartupID<StartupExceptionHandler>()
        override fun build(pool: StartupPool): StartupExceptionHandler = StartupExceptionHandler(pool, crashKey, handler)
    }

    init {
        setupPlatformExceptionHandler(crashKey, handler)
    }
}