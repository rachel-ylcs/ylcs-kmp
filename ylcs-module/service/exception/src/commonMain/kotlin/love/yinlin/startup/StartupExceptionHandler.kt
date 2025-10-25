package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArg
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupDoc
import love.yinlin.service.SyncStartup

@StartupDoc(
    StartupArg(0, "crashKey", String::class),
    StartupArg(1, "handler", StartupExceptionHandler.Handler::class)
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

@StartupDoc(
    StartupArg(0, "crashKey", String::class),
    StartupArg(1, "handler", StartupExceptionHandler.Handler::class)
)
expect fun buildStartupExceptionHandler(): StartupExceptionHandler