package love.yinlin.service

import love.yinlin.platform.Coroutines
import kotlin.jvm.JvmName

open class BasicService {
    protected val startups = mutableListOf<StartupDelegate<out Startup>>()

    @JvmName("serviceSync")
    protected inline fun <reified S : SyncStartup> service(noinline factory: () -> S, vararg args: Any?) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Sync, factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("serviceASync")
    protected inline fun <reified S : AsyncStartup> service(noinline factory: () -> S, vararg args: Any?) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Async, factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("serviceFree")
    protected inline fun <reified S : FreeStartup> service(noinline factory: () -> S, vararg args: Any?) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupType.Free, factory, arrayOf(*args))
        startups += delegate
        return delegate
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

        for (delegate in free) initAsync(context, delegate)
        for (delegate in sync) initSync(context, delegate)
        for (delegate in async) initAsync(context, delegate)
    }

    fun init(context: PlatformContext) {
        val (system, user) = startups.partition { it.privilege == StartupPrivilege.System }
        initStartups(context, system)
        initStartups(context, user)
    }
}