package love.yinlin.startup

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArg
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupHandler
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
import kotlin.experimental.ExperimentalNativeApi

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
)
actual fun buildStartupExceptionHandler(): StartupExceptionHandler = object : StartupExceptionHandler() {
    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    override fun init(context: PlatformContext, args: StartupArgs) {
        super.init(context, args)
        val handler: Handler = args[1]
        setUnhandledExceptionHook { e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
        val exceptionHandler: CPointer<NSUncaughtExceptionHandler> = staticCFunction { e ->
            if (e != null) handler.handle(crashKey, Throwable(e.reason), "${e.reason}\n${e.callStackSymbols.joinToString(",")}")
        }
        NSSetUncaughtExceptionHandler(exceptionHandler)
    }
}