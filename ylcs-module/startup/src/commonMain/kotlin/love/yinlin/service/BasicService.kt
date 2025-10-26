package love.yinlin.service

import love.yinlin.platform.Coroutines
import kotlin.jvm.JvmName

open class BasicService {
    protected val startups = mutableListOf<StartupDelegate<out Startup>>()

    @JvmName("serviceSync")
    protected inline fun <reified S : SyncStartup> service(vararg args: Any?, order: Int = StartupDelegate.DEFAULT_ORDER, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Sync, factory, arrayOf(*args), order)
        startups += delegate
        return delegate
    }

    @JvmName("serviceASync")
    protected inline fun <reified S : AsyncStartup> service(vararg args: Any?, order: Int = StartupDelegate.DEFAULT_ORDER, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Async, factory, arrayOf(*args), order)
        startups += delegate
        return delegate
    }

    @JvmName("serviceFree")
    protected inline fun <reified S : FreeStartup> service(vararg args: Any?, order: Int = StartupDelegate.DEFAULT_ORDER, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Free, factory, arrayOf(*args), order)
        startups += delegate
        return delegate
    }

    protected fun sync(vararg args: Any?, order: Int = StartupDelegate.DEFAULT_ORDER, factory: (StartupArgs) -> Unit) = service<SyncStartup>(*args, order = order) {
        SyncStartup { _, startupArgs -> factory(startupArgs) }
    }

    protected fun async(vararg args: Any?, order: Int = StartupDelegate.DEFAULT_ORDER, factory: suspend (StartupArgs) -> Unit) = service<AsyncStartup>(*args, order = order) {
        AsyncStartup { _, startupArgs -> factory(startupArgs) }
    }

    protected fun free(vararg args: Any?, order: Int = StartupDelegate.DEFAULT_ORDER, factory: suspend (StartupArgs) -> Unit) = service<FreeStartup>(*args, order = order) {
        FreeStartup { _, startupArgs -> factory(startupArgs) }
    }

    private fun initSync(context: PlatformContext, delegate: StartupDelegate<out Startup>) {
        delegate.create()
        delegate.init(context)
    }

    private fun initAsync(context: PlatformContext, delegate: StartupDelegate<out Startup>) {
        delegate.create()
        Coroutines.startMain { delegate.initAsync(context) }
    }

    private fun initStartups(context: PlatformContext, items: List<StartupDelegate<out Startup>>) {
        val (sync, other) = items.partition { it.type == StartupType.Sync }
        val (free, async) = other.partition { it.type == StartupType.Free }

        for (delegate in free.sortedBy { it.order }) initAsync(context, delegate)
        for (delegate in sync.sortedBy { it.order }) initSync(context, delegate)
        for (delegate in async.sortedBy { it.order }) initAsync(context, delegate)
    }

    fun init(context: PlatformContext) {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        initStartups(context, system)
        initStartups(context, user)
    }
}