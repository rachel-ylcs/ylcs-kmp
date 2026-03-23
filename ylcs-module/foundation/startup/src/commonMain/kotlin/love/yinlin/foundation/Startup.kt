package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class Startup(val context: PlatformContextProvider) {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(${this.metaClassName})"

    abstract fun init(scope: CoroutineScope, args: StartupArgs)
    abstract suspend fun CoroutineScope.init(args: StartupArgs)
    open suspend fun CoroutineScope.initLater(args: StartupArgs) { }
    open fun destroy(args: StartupArgs) { }
    open fun destroyBefore(args: StartupArgs) { }

    open val canSafeAccess: Boolean = true

    internal val errorScope: Nothing get() = error("$this cannot be used in this scope")
}