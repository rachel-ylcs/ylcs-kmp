package love.yinlin.foundation.exception

fun interface ExceptionHandler {
    fun handle(key: String, e: Throwable, error: String)

    companion object {
        fun setup(crashKey: String, handler: ExceptionHandler): AutoCloseable = setupPlatformExceptionHandler(crashKey, handler)
    }
}