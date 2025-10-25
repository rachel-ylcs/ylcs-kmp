package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArg
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupHandler
import love.yinlin.service.SyncStartup

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
)
abstract class StartupExceptionHandler : SyncStartup {
    fun interface Handler {
        fun handle(key: String, e: Throwable, error: String)
    }

    lateinit var crashKey: String
        private set

    override fun init(context: PlatformContext, args: StartupArgs) {
        crashKey = args[0]
    }
}

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
)
expect fun buildStartupExceptionHandler(): StartupExceptionHandler