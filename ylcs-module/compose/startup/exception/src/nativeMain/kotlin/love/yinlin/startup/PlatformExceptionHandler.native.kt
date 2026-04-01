package love.yinlin.startup

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler) {
    setUnhandledExceptionHook { e ->
        handler.handle(crashKey, e, e.stackTraceToString())
    }
}