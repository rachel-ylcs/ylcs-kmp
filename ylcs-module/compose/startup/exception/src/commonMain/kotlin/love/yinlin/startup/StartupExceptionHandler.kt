package love.yinlin.startup

import love.yinlin.coroutines.cpuContext
import love.yinlin.foundation.Startup
import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import kotlin.coroutines.CoroutineContext

class StartupExceptionHandler(
    pool: StartupPool,
    val crashKey: String,
    private val handler: ExceptionHandler
) : Startup(pool) {
    class Factory(
        private val crashKey: String,
        private val handler: ExceptionHandler
    ) : StartupFactory<StartupExceptionHandler> {
        override val id: String = StartupID<StartupExceptionHandler>()
        override val dependencies: List<String> = emptyList()
        override val dispatcher: CoroutineContext = cpuContext
        override fun build(pool: StartupPool): StartupExceptionHandler = StartupExceptionHandler(pool, crashKey, handler)
    }

    override suspend fun init() {
        setupPlatformExceptionHandler(crashKey, handler)
    }
}