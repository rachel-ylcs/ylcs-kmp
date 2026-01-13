package love.yinlin

import kotlinx.coroutines.CoroutineScope

abstract class AsyncStartup : Startup() {
    override fun toString(): String = "(Async|${this::class.qualifiedName})"

    override suspend fun CoroutineScope.initLater(context: Context, args: StartupArgs) { }

    final override fun init(context: Context, args: StartupArgs) = errorScope
    final override fun initLater(context: Context, args: StartupArgs) = errorScope

    companion object {
        inline fun build(crossinline factory: suspend (StartupArgs) -> Unit): AsyncStartup = object : AsyncStartup() {
            override suspend fun CoroutineScope.init(context: Context, args: StartupArgs) {
                factory(args)
            }
        }
    }
}