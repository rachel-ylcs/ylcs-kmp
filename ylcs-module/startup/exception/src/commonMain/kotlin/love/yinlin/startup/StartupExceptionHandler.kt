package love.yinlin.startup

import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupHandler
import love.yinlin.foundation.SyncStartup

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
)
expect class StartupExceptionHandler() : SyncStartup {
    fun interface Handler {
        fun handle(key: String, e: Throwable, error: String)
    }

    val crashKey: String

    override fun init(context: Context, args: StartupArgs)
}