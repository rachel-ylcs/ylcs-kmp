package love.yinlin.foundation.exception

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import love.yinlin.concurrent.Lock
import platform.Foundation.NSGetUncaughtExceptionHandler
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
import kotlin.experimental.ExperimentalNativeApi

private val handlerLock = Lock()
private var activeHandler: Pair<String, ExceptionHandler>? = null

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
internal actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler): AutoCloseable {
    val binding = crashKey to handler
    val installed: (Throwable) -> Unit = { error -> handler.handle(crashKey, error, error.stackTraceToString()) }
    val previous = setUnhandledExceptionHook(installed)
    val previousObjC = NSGetUncaughtExceptionHandler()
    handlerLock.synchronized { activeHandler = binding }
    val installedObjC: CPointer<NSUncaughtExceptionHandler> = staticCFunction { error ->
        val active = handlerLock.synchronized { activeHandler }
        if (active != null && error != null) {
            active.second.handle(active.first, Throwable(error.reason), "${error.reason}\n${error.callStackSymbols.joinToString(",")}")
        }
    }
    NSSetUncaughtExceptionHandler(installedObjC)
    return AutoCloseable {
        if (getUnhandledExceptionHook() === installed) setUnhandledExceptionHook(previous)
        if (NSGetUncaughtExceptionHandler() == installedObjC) NSSetUncaughtExceptionHandler(previousObjC)
        handlerLock.synchronized {
            if (activeHandler === binding) activeHandler = null
        }
    }
}
