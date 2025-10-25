package love.yinlin.startup

import kotlinx.browser.window
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
    @OptIn(ExperimentalWasmJsInterop::class)
    override fun init(context: PlatformContext, args: StartupArgs) {
        super.init(context, args)
        val handler: Handler = args[1]
        window.onerror = { message, source, lineno, colno, error ->
            val e = error?.toThrowableOrNull() ?: Throwable(message?.toString() ?: "error")
            val errorString = "$source - $lineno - $colno\n$message\n$e"
            handler.handle(crashKey, e, errorString)
            false.toJsBoolean()
        }
    }
}