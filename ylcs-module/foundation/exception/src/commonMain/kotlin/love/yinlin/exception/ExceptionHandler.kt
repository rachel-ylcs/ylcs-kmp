package love.yinlin.exception

fun interface ExceptionHandler {
    fun handle(key: String, e: Throwable, error: String)
}