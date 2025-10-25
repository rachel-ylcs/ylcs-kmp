package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArg
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupDoc

@StartupDoc(
    StartupArg(0, "crashKey", String::class),
    StartupArg(1, "handler", StartupExceptionHandler.Handler::class)
)
actual fun buildStartupExceptionHandler(): StartupExceptionHandler = object : StartupExceptionHandler() {
    override fun init(context: PlatformContext, args: StartupArgs) {
        super.init(context, args)
        val handler: Handler = args[1]
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
    }
}