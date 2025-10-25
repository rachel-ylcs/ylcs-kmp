package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArg
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupHandler

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
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