package love.yinlin.service

import love.yinlin.platform.Coroutines
import kotlin.jvm.JvmName

open class Service {
    protected val startups = mutableListOf<StartupDelegate<out Startup>>()

    @JvmName("serviceSync")
    protected inline fun <reified S : SyncStartup> service(noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Sync, factory)
        startups += delegate
        return delegate
    }

    @JvmName("serviceASync")
    protected inline fun <reified S : AsyncStartup> service(noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Async, factory)
        startups += delegate
        return delegate
    }

    @JvmName("serviceFree")
    protected inline fun <reified S : FreeStartup> service(noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Free, factory)
        startups += delegate
        return delegate
    }

    private fun initSync(delegate: StartupDelegate<out Startup>) {
        delegate.create()
        delegate.init()
    }

    private fun initAsync(delegate: StartupDelegate<out Startup>) {
        delegate.create()
        Coroutines.startMain { delegate.initAsync() }
    }

    private fun initStartups(items: List<StartupDelegate<out Startup>>) {
        val (sync, other) = items.partition { it.type == StartupType.Sync }
        val (free, async) = other.partition { it.type == StartupType.Free }

        for (delegate in free) initAsync(delegate)
        for (delegate in sync) initSync(delegate)
        for (delegate in async) initAsync(delegate)
    }

    fun init() {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        initStartups(system)
        initStartups(user)
    }
}