package love.yinlin

import love.yinlin.platform.Coroutines
import kotlin.jvm.JvmName

open class Service {
    protected val startups = mutableListOf<StartupDelegate<out Startup>>()

    @JvmName("systemSync")
    protected inline fun <reified S : SyncStartup> system(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Sync, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    @JvmName("systemASync")
    protected inline fun <reified S : AsyncStartup> system(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Async, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    @JvmName("systemFree")
    protected inline fun <reified S : FreeStartup> system(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Free, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    @JvmName("serviceSync")
    protected inline fun <reified S : SyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Sync, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    @JvmName("serviceASync")
    protected inline fun <reified S : AsyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Async, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    @JvmName("serviceFree")
    protected inline fun <reified S : FreeStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Free, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    protected fun sync(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: (StartupArgs) -> Unit) = service<SyncStartup>(*args, priority = priority) {
        SyncStartup { _, startupArgs -> factory(startupArgs) }
    }

    protected fun async(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: suspend (StartupArgs) -> Unit) = service<AsyncStartup>(*args, priority = priority) {
        AsyncStartup { _, startupArgs -> factory(startupArgs) }
    }

    protected fun free(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, factory: suspend (StartupArgs) -> Unit) = service<FreeStartup>(*args, priority = priority) {
        FreeStartup { _, startupArgs -> factory(startupArgs) }
    }

    private fun initSync(context: Context, delegate: StartupDelegate<out Startup>, delay: Boolean) {
        if (!delay) delegate.create()
        delegate.initSync(context, delay)
    }

    private fun destroy(context: Context, delegate: StartupDelegate<out Startup>, delay: Boolean) {
        delegate.destroy(context, delay)
    }

    private fun initAsync(context: Context, delegate: StartupDelegate<out Startup>, delay: Boolean) {
        if (!delay) delegate.create()
        Coroutines.startMain { delegate.initAsync(context, delay) }
    }

    private fun initStartups(context: Context, items: List<StartupDelegate<out Startup>>, delay: Boolean) {
        val (sync, other) = items.partition { it.type == StartupType.Sync }
        val (free, async) = other.partition { it.type == StartupType.Free }

        for (delegate in free.sortedByDescending { it.priority }) initAsync(context, delegate, delay)
        for (delegate in sync.sortedByDescending { it.priority }) initSync(context, delegate, delay)
        for (delegate in async.sortedByDescending { it.priority }) initAsync(context, delegate, delay)
    }

    private fun destoryStartups(context: Context, items: List<StartupDelegate<out Startup>>, delay: Boolean) {
        val (sync, other) = items.partition { it.type == StartupType.Sync }
        // free服务不允许析构, 因为其构造时是任意时刻, 而程序析构时是同步的, 无法确定其析构顺序
        val (async, _) = other.partition { it.type == StartupType.Async }
        // 这里必须先倒序排列再反向才能提供正确的析构顺序, 因为 sortedByDescending 排序是稳定的
        for (delegate in sync.sortedByDescending { it.priority }.reversed()) destroy(context, delegate, delay)
        for (delegate in async.sortedByDescending { it.priority }.reversed()) destroy(context, delegate, delay)
    }

    protected fun initService(context: Context, delay: Boolean) {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        initStartups(context, system, delay)
        initStartups(context, user, delay)
    }

    protected fun destroyService(context: Context, delay: Boolean) {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        destoryStartups(context, system, delay)
        destoryStartups(context, user, delay)
    }
}