package love.yinlin.foundation

sealed class Startup(val pool: StartupPool) {
    abstract suspend fun init()
    open suspend fun initLater() { }
    open fun destroy() { }
    open fun destroyBefore() { }
}