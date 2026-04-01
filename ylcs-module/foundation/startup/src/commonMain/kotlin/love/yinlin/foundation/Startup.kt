package love.yinlin.foundation

abstract class Startup(protected val pool: StartupPool) {
    abstract suspend fun init()
    open suspend fun initLater() { }
    open fun destroy() { }
    open fun destroyBefore() { }

    open val canSafeAccess: Boolean = true
}