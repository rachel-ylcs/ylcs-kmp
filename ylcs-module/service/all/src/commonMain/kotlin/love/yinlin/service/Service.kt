package love.yinlin.service

import love.yinlin.data.AppInfo
import love.yinlin.startup.StartupContext
import kotlin.jvm.JvmName

open class Service(appInfo: AppInfo) : BasicService() {
    val context by system(::StartupContext)

    @JvmName("systemSync")
    private inline fun <reified S : SyncStartup> system(noinline factory: () -> S, vararg args: Any?) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupPrivilege.System, StartupType.Sync, factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("systemASync")
    private inline fun <reified S : AsyncStartup> system(noinline factory: () -> S, vararg args: Any?) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupPrivilege.System, StartupType.Async, factory, arrayOf(*args))
        startups += delegate
        return delegate
    }

    @JvmName("systemFree")
    private inline fun <reified S : FreeStartup> system(noinline factory: () -> S, vararg args: Any?) : StartupDelegate<S> {
        val delegate = StartupDelegate(StartupPrivilege.System, StartupType.Free, factory, arrayOf(*args))
        startups += delegate
        return delegate
    }
}