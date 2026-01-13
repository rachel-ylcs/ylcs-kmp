package love.yinlin

import kotlinx.coroutines.CoroutineScope

abstract class Startup {
    override fun toString(): String = "(${this::class.qualifiedName})"

    abstract fun init(context: Context, args: StartupArgs)
    abstract fun initLater(context: Context, args: StartupArgs)
    abstract suspend fun CoroutineScope.init(context: Context, args: StartupArgs)
    abstract suspend fun CoroutineScope.initLater(context: Context, args: StartupArgs)
    open fun destroy(context: Context, args: StartupArgs) { }
    open fun destroyBefore(context: Context, args: StartupArgs) { }

    val errorScope: Nothing get() = error("$this cannot be used in this scope")
}