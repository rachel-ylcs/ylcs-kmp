package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class FreeStartup : Startup() {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(Free|${this.metaClassName})"

    override suspend fun CoroutineScope.initLater(context: Context, args: StartupArgs) { }

    final override fun init(context: Context, args: StartupArgs) = errorScope
    final override fun initLater(context: Context, args: StartupArgs) = errorScope
    final override fun destroy(context: Context, args: StartupArgs) = errorScope
    final override fun destroyBefore(context: Context, args: StartupArgs) = errorScope

    companion object {
        inline fun build(crossinline factory: suspend (StartupArgs) -> Unit): FreeStartup = object : FreeStartup() {
            override suspend fun CoroutineScope.init(context: Context, args: StartupArgs) {
                factory(args)
            }
        }
    }
}