package love.yinlin.service

fun interface AsyncStartup : Startup {
    suspend fun init()
}