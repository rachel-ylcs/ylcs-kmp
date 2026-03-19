package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class AsyncStartup(context: PlatformContextProvider) : Startup(context) {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(Async|${this.metaClassName})"

    final override fun init(scope: CoroutineScope, args: StartupArgs) = errorScope

    companion object {
        @PublishedApi
        internal inline fun build(context: PlatformContextProvider, crossinline factory: suspend CoroutineScope.(StartupArgs) -> Unit): AsyncStartup = object : AsyncStartup(context) {
            override suspend fun CoroutineScope.init(args: StartupArgs) {
                factory(args)
            }
        }
    }
}