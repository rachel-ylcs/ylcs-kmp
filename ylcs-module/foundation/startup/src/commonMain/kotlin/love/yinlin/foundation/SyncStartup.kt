package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope

abstract class SyncStartup : Startup() {
    override fun toString(): String = "(Sync|${this::class.qualifiedName})"

    override fun initLater(context: Context, args: StartupArgs) {}

    final override suspend fun CoroutineScope.init(context: Context, args: StartupArgs) = errorScope
    final override suspend fun CoroutineScope.initLater(context: Context, args: StartupArgs) = errorScope

    companion object {
        inline fun build(crossinline factory: (StartupArgs) -> Unit): SyncStartup = object : SyncStartup() {
            override fun init(context: Context, args: StartupArgs) {
                factory(args)
            }
        }
    }
}