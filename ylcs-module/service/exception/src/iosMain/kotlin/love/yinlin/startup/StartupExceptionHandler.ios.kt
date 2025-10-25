package love.yinlin.startup

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import love.yinlin.service.PlatformContext
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
import kotlin.experimental.ExperimentalNativeApi

actual fun buildStartupExceptionHandler(): StartupExceptionHandler = object : StartupExceptionHandler() {
    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    override fun init(context: PlatformContext, args: Array<Any?>) {
        super.init(context, args)
        val handler = args[1] as Handler
        setUnhandledExceptionHook { e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
        val exceptionHandler: CPointer<NSUncaughtExceptionHandler> = staticCFunction { e ->
            if (e != null) handler.handle(crashKey, Throwable(e.reason), "${e.reason}\n${e.callStackSymbols.joinToString(",")}")
        }
        NSSetUncaughtExceptionHandler(exceptionHandler)
    }
}