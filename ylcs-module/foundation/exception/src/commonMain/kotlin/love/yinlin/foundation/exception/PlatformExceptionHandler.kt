package love.yinlin.foundation.exception

internal expect fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler): AutoCloseable
