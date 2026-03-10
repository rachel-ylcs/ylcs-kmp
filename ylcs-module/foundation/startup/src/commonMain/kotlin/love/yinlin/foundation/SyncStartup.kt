package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class SyncStartup : Startup() {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(Sync|${this.metaClassName})"

    final override suspend fun CoroutineScope.init(context: Context, args: StartupArgs) = errorScope

    companion object {
        @PublishedApi
        internal inline fun build(crossinline factory: CoroutineScope.(StartupArgs) -> Unit): SyncStartup = object : SyncStartup() {
            override fun init(scope: CoroutineScope, context: Context, args: StartupArgs) {
                scope.factory(args)
            }
        }
    }
}