package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.reflect.metaClassName

abstract class SyncStartup : Startup() {
    @OptIn(CompatibleRachelApi::class)
    override fun toString(): String = "(Sync|${this.metaClassName})"

    override fun initLater(context: Context, args: StartupArgs) {}

    final override suspend fun init(scope: CoroutineScope, context: Context, args: StartupArgs) = errorScope
    final override suspend fun initLater(scope: CoroutineScope, context: Context, args: StartupArgs) = errorScope

    companion object {
        internal inline fun build(crossinline factory: (StartupArgs) -> Unit): SyncStartup = object : SyncStartup() {
            override fun init(context: Context, args: StartupArgs) {
                factory(args)
            }
        }
    }
}