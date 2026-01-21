package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class Startup {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(${this.metaClassName})"

    abstract fun init(context: Context, args: StartupArgs)
    abstract fun initLater(context: Context, args: StartupArgs)
    abstract suspend fun CoroutineScope.init(context: Context, args: StartupArgs)
    abstract suspend fun CoroutineScope.initLater(context: Context, args: StartupArgs)
    open fun destroy(context: Context, args: StartupArgs) { }
    open fun destroyBefore(context: Context, args: StartupArgs) { }

    val errorScope: Nothing get() = error("$this cannot be used in this scope")
}