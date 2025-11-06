package love.yinlin

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope

@Stable
abstract class Startup {
    open val privilege: StartupPrivilege get() = StartupPrivilege.User

    override fun toString(): String = "($privilege|${this::class.qualifiedName})"

    abstract fun init(context: Context, args: StartupArgs)
    abstract fun initLater(context: Context, args: StartupArgs)
    abstract suspend fun CoroutineScope.init(context: Context, args: StartupArgs)
    abstract suspend fun CoroutineScope.initLater(context: Context, args: StartupArgs)
    open fun destroy(context: Context, args: StartupArgs) { }
    open fun destroyBefore(context: Context, args: StartupArgs) { }

    val errorScope: Nothing get() = error("$this cannot be used in this scope")
}