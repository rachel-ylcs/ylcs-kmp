package love.yinlin.exception

internal expect fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler): AutoCloseable
