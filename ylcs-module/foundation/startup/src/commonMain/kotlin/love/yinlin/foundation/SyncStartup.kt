package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class SyncStartup(context: PlatformContextProvider) : Startup(context) {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(Sync|${this.metaClassName})"

    final override suspend fun CoroutineScope.init(args: StartupArgs) = errorScope

    companion object {
        @PublishedApi
        internal inline fun build(context: PlatformContextProvider, crossinline factory: CoroutineScope.(StartupArgs) -> Unit): SyncStartup = object : SyncStartup(context) {
            override fun init(scope: CoroutineScope, args: StartupArgs) {
                scope.factory(args)
            }
        }
    }
}