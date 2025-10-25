package love.yinlin.service

import love.yinlin.data.AppInfo
import love.yinlin.startup.*
import kotlin.jvm.JvmName

open class Service(appInfo: AppInfo) : BasicService() {
    val context by system(factory = ::StartupContext)
    val os by system(appInfo.appName, factory = ::StartupOS)

    @JvmName("systemSync")
    private inline fun <reified S : SyncStartup> system(vararg args: Any?, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Sync, arrayOf(*args), factory)
        startups += delegate
        return delegate
    }

    @JvmName("systemASync")
    private inline fun <reified S : AsyncStartup> system(vararg args: Any?, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Async, arrayOf(*args), factory)
        startups += delegate
        return delegate
    }

    @JvmName("systemFree")
    private inline fun <reified S : FreeStartup> system(vararg args: Any?, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Free, arrayOf(*args), factory)
        startups += delegate
        return delegate
    }
}