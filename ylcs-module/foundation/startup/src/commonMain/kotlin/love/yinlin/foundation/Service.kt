package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.coroutines.mainContext
import kotlin.jvm.JvmName

open class Service {
    @PublishedApi internal val startups = mutableListOf<StartupDelegate<out Startup>>()

    @JvmName("serviceSync")
    protected inline fun <reified S : SyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(type = StartupType.Sync, priority = priority, factory = factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("serviceASync")
    protected inline fun <reified S : AsyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(type = StartupType.Async, priority = priority, factory = factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("serviceFree")
    protected inline fun <reified S : FreeStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(type = StartupType.Free, priority = priority, factory = factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    protected fun sync(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: (StartupArgs) -> Unit) =
        service<SyncStartup>(*args, priority = priority) { SyncStartup.build(factory) }

    protected fun async(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: suspend CoroutineScope.(StartupArgs) -> Unit) =
        service<AsyncStartup>(*args, priority = priority) { AsyncStartup.build(factory) }

    protected fun free(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: suspend CoroutineScope.(StartupArgs) -> Unit) =
        service<FreeStartup>(*args, priority = priority) { FreeStartup.build(factory) }

    private val split: Array<List<StartupDelegate<out Startup>>> get() {
        val (sync, other) = startups.partition { it.isSync }
        val (async, free) = other.partition { it.isAsync }
        return arrayOf(sync, async, free)
    }

    protected fun initService(scope: CoroutineScope, context: Context, later: Boolean, immediate: Boolean) {
        val (sync, async, free) = split

        for (delegate in free.sortedByDescending { it.priority }) {
            scope.launch(mainContext) {
                delegate.initStartup(this, context, later)
            }
        }

        for (delegate in sync.sortedByDescending { it.priority }) {
            delegate.initStartup(context, later)
        }

        scope.launch(mainContext) {
            for (delegate in async.sortedByDescending { it.priority }) {
                delegate.initStartup(this, context, later)
            }
            if (immediate && !later) initService(this, context, later = true, immediate = false)
        }
    }

    protected fun destroyService(context: Context, before: Boolean) {
        // free服务不允许析构, 因为其构造时是任意时刻, 而程序析构时是同步的, 无法确定其析构顺序
        val (sync, async, _) = split
        // 这里必须先倒序排列再反向才能提供正确的析构顺序, 因为 sortedByDescending 排序是稳定的
        for (delegate in async.sortedByDescending { it.priority }.asReversed()) delegate.destroyStartup(context, before)
        for (delegate in sync.sortedByDescending { it.priority }.asReversed()) delegate.destroyStartup(context, before)
    }
}