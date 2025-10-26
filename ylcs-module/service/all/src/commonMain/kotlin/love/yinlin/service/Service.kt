package love.yinlin.service

import love.yinlin.startup.*
import kotlin.jvm.JvmName

open class Service : BasicService() {
    val context by system(factory = ::StartupContext)

    @JvmName("systemSync")
    private inline fun <reified S : SyncStartup> system(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Sync, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    @JvmName("systemASync")
    private inline fun <reified S : AsyncStartup> system(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Async, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }

    @JvmName("systemFree")
    private inline fun <reified S : FreeStartup> system(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate.system(StartupType.Free, factory, arrayOf(*args), priority)
        startups += delegate
        return delegate
    }
}