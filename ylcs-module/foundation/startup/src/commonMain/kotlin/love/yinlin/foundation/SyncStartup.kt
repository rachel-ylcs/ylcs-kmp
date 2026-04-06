package love.yinlin.foundation

open class SyncStartup(pool: StartupPool) : Startup(pool) {
    final override suspend fun init() { }
}