package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class AsyncStartup : Startup() {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(Async|${this.metaClassName})"

    final override fun init(scope: CoroutineScope, context: Context, args: StartupArgs) = errorScope

    companion object {
        @PublishedApi
        internal inline fun build(crossinline factory: suspend CoroutineScope.(StartupArgs) -> Unit): AsyncStartup = object : AsyncStartup() {
            override suspend fun CoroutineScope.init(context: Context, args: StartupArgs) {
                factory(args)
            }
        }
    }
}