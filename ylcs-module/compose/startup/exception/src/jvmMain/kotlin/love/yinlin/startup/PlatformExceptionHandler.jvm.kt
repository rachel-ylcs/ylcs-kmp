package love.yinlin.startup

actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        handler.handle(crashKey, e, e.stackTraceToString())
    }
}