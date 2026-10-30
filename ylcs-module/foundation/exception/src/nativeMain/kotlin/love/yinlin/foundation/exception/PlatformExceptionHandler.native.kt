package love.yinlin.foundation.exception

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler): AutoCloseable {
    val installed: (Throwable) -> Unit = { error -> handler.handle(crashKey, error, error.stackTraceToString()) }
    val previous = setUnhandledExceptionHook(installed)
    return AutoCloseable {
        if (getUnhandledExceptionHook() === installed) setUnhandledExceptionHook(previous)
    }
}
