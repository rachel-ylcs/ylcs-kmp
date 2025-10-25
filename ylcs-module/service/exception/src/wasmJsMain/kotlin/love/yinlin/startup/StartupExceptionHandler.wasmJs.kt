package love.yinlin.startup

import kotlinx.browser.window
import love.yinlin.service.PlatformContext

actual fun buildStartupExceptionHandler(): StartupExceptionHandler = object : StartupExceptionHandler() {
    @OptIn(ExperimentalWasmJsInterop::class)
    override fun init(context: PlatformContext, args: Array<Any?>) {
        super.init(context, args)
        val handler = args[1] as Handler
        window.onerror = { message, source, lineno, colno, error ->
            val e = error?.toThrowableOrNull() ?: Throwable(message?.toString() ?: "error")
            val errorString = "$source - $lineno - $colno\n$message\n$e"
            handler.handle(crashKey, e, errorString)
            false.toJsBoolean()
        }
    }
}