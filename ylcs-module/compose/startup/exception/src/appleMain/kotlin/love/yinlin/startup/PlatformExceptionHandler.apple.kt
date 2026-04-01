package love.yinlin.startup

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
import kotlin.experimental.ExperimentalNativeApi

// staticCFunction must take an unbound, non-capturing function or lambda
private var GlobalKey: String? = null
private var GlobalHandler: ExceptionHandler? = null

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler) {
    GlobalKey = crashKey
    GlobalHandler = handler

    setUnhandledExceptionHook { e ->
        handler.handle(crashKey, e, e.stackTraceToString())
    }

    val exceptionHandler: CPointer<NSUncaughtExceptionHandler> = staticCFunction { e ->
        val globalHandler = GlobalHandler
        val globalKey = GlobalKey
        if (globalHandler != null && globalKey != null && e != null) {
            globalHandler.handle(globalKey, Throwable(e.reason), "${e.reason}\n${e.callStackSymbols.joinToString(",")}")
        }
    }
    NSSetUncaughtExceptionHandler(exceptionHandler)
}