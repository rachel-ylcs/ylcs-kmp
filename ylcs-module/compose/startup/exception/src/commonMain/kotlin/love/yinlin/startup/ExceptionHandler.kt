package love.yinlin.startup

fun interface ExceptionHandler {
    fun handle(key: String, e: Throwable, error: String)
}