package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class AsyncStartup : Startup() {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(Async|${this.metaClassName})"

    override suspend fun initLater(scope: CoroutineScope, context: Context, args: StartupArgs) { }

    final override fun init(context: Context, args: StartupArgs) = errorScope
    final override fun initLater(context: Context, args: StartupArgs) = errorScope

    companion object {
        internal inline fun build(crossinline factory: suspend CoroutineScope.(StartupArgs) -> Unit): AsyncStartup = object : AsyncStartup() {
            override suspend fun init(scope: CoroutineScope, context: Context, args: StartupArgs) {
                scope.factory(args)
            }
        }
    }
}