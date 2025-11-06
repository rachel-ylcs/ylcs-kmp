package love.yinlin

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope

@Stable
abstract class FreeStartup : Startup() {
    override fun toString(): String = "($privilege|Free|${this::class.qualifiedName})"

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