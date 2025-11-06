package love.yinlin

import androidx.compose.runtime.Stable
import kotlinx.coroutines.launch
import love.yinlin.platform.Coroutines
import kotlin.jvm.JvmName

@Stable
open class Service {
    protected val startups = mutableListOf<StartupDelegate<out Startup>>()

    @JvmName("serviceSync")
    protected inline fun <reified S : SyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(priority = priority, factory = factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("serviceASync")
    protected inline fun <reified S : AsyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(priority = priority, factory = factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("serviceFree")
    protected inline fun <reified S : FreeStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(priority = priority, factory = factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    protected fun sync(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: (StartupArgs) -> Unit) =
        service<SyncStartup>(*args, priority = priority) { SyncStartup.build(factory) }

    protected fun async(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: suspend (StartupArgs) -> Unit) =
        service<AsyncStartup>(*args, priority = priority) { AsyncStartup.build(factory) }

    protected fun free(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: suspend (StartupArgs) -> Unit) =
        service<FreeStartup>(*args, priority = priority) { FreeStartup.build(factory) }

    private fun initStartups(context: Context, items: List<StartupDelegate<out Startup>>, later: Boolean) {
        val (sync, other) = items.partition { it.isSync}
        val (async, free) = other.partition { it.isAsync }

        Coroutines.startMain {
            for (delegate in free.sortedByDescending { it.priority }) {
                launch { with(delegate) { this@launch.initStartup(context, later) } }
            }
        }

        for (delegate in sync.sortedByDescending { it.priority }) {
            delegate.initStartup(context, later)
        }

        Coroutines.startMain {
            for (delegate in async.sortedByDescending { it.priority }) {
                with(delegate) { this@startMain.initStartup(context, later) }
            }
        }
    }

    private fun destoryStartups(context: Context, items: List<StartupDelegate<out Startup>>, before: Boolean) {
        // free服务不允许析构, 因为其构造时是任意时刻, 而程序析构时是同步的, 无法确定其析构顺序
        val (sync, other) = items.partition { it.isSync }
        val (async, _) = other.partition { it.isAsync }
        // 这里必须先倒序排列再反向才能提供正确的析构顺序, 因为 sortedByDescending 排序是稳定的
        for (delegate in sync.sortedByDescending { it.priority }.reversed()) delegate.destroyStartup(context, before)
        for (delegate in async.sortedByDescending { it.priority }.reversed()) delegate.destroyStartup(context, before)
    }

    protected fun createService() {
        for (delegate in startups) delegate.createStartup()
    }

    protected fun initService(context: Context, later: Boolean) {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        initStartups(context, system, later)
        initStartups(context, user, later)
    }

    protected fun destroyService(context: Context, before: Boolean) {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        destoryStartups(context, system, before)
        destoryStartups(context, user, before)
    }
}