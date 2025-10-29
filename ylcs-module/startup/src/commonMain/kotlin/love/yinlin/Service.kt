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

    private fun initSync(context: Context, delegate: StartupDelegate<out Startup>) {
        delegate.create()
        delegate.init(context)
    }

    private fun initAsync(context: Context, delegate: StartupDelegate<out Startup>) {
        delegate.create()
        Coroutines.startMain { delegate.initAsync(context) }
    }

    private fun initStartups(context: Context, items: List<StartupDelegate<out Startup>>) {
        val (sync, other) = items.partition { it.type == StartupType.Sync }
        val (free, async) = other.partition { it.type == StartupType.Free }

        for (delegate in free.sortedByDescending { it.priority }) initAsync(context, delegate)
        for (delegate in sync.sortedByDescending { it.priority }) initSync(context, delegate)
        for (delegate in async.sortedByDescending { it.priority }) initAsync(context, delegate)
    }

    protected fun initService(context: Context) {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        initStartups(context, system)
        initStartups(context, user)
    }
}