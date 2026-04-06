package love.yinlin.foundation

open class AsyncStartup(pool: StartupPool) : Startup(pool) {
    override suspend fun init() { }
}