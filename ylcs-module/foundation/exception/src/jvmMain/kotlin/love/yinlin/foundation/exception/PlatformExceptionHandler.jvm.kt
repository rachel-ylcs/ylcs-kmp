package love.yinlin.foundation.exception

internal actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler): AutoCloseable {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    val installed = Thread.UncaughtExceptionHandler { _, error ->
        handler.handle(crashKey, error, error.stackTraceToString())
    }
    Thread.setDefaultUncaughtExceptionHandler(installed)
    return AutoCloseable {
        if (Thread.getDefaultUncaughtExceptionHandler() === installed) Thread.setDefaultUncaughtExceptionHandler(previous)
    }
}
