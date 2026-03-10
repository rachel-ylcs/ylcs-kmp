package love.yinlin.foundation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catchingError
import kotlin.jvm.JvmName

open class Service {
    @PublishedApi internal val startupList = mutableListOf<StartupDelegate<out Startup>>()

    inline fun <reified T : Startup> startup(): T? {
        for (delegate in startupList) {
            val unsafeStartup = delegate.unsafeStartup
            if (unsafeStartup is T) {
                return if (unsafeStartup.isSafeAccess) unsafeStartup else null
            }
        }
        return null
    }

    @JvmName("serviceSync")
    protected inline fun <reified S : SyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, name: String? = null, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(type = StartupType.Sync, priority = priority, name = name, factory = factory, arrayOf(*args))
        startupList += delegate
        return delegate
    }

    @JvmName("serviceASync")
    protected inline fun <reified S : AsyncStartup> service(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, name: String? = null, noinline factory: () -> S) : StartupDelegate<S> {
        val delegate = StartupDelegate(type = StartupType.Async, priority = priority, name = name, factory = factory, arrayOf(*args))
        startupList += delegate
        return delegate
    }

    protected inline fun sync(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, name: String? = null, crossinline factory: CoroutineScope.(StartupArgs) -> Unit) =
        service<SyncStartup>(*args, priority = priority, name = name) { SyncStartup.build(factory) }

    protected inline fun async(vararg args: Any?, priority: Int = StartupDelegate.DEFAULT, name: String? = null, crossinline factory: suspend CoroutineScope.(StartupArgs) -> Unit) =
        service<AsyncStartup>(*args, priority = priority, name = name) { AsyncStartup.build(factory) }

    protected fun initService(scope: CoroutineScope, context: Context) {
        val (sync, async) = startupList.partition { it.isSync }

        for (delegate in sync.sortedByDescending { it.priority }) {
            catchingError {
                delegate.createStartup()
                delegate.initStartup(scope, context)
            }?.let { throw IllegalStateException("SyncStartup initialize failed: $delegate", it) }
        }

        val asyncStartupList = async.sortedByDescending { it.priority }
        for (delegate in asyncStartupList) delegate.createStartup()
        scope.launch(mainContext) {
            for (delegate in asyncStartupList) {
                catchingError {
                    with(delegate) { initStartup(context) }
                }?.let { throw IllegalStateException("AsyncStartup initialize failed: $delegate", it) }
            }
        }
    }

    protected suspend fun CoroutineScope.initServiceLater(context: Context) {
        val (sync, async) = startupList.partition { it.isSync }

        for (delegate in sync.sortedByDescending { it.priority }) {
            catchingError {
                with(delegate) { initStartupLater(context) }
            }?.let { throw IllegalStateException("SyncStartup initialize later failed: $delegate", it) }
        }

        for (delegate in async.sortedByDescending { it.priority }) {
            catchingError {
                with(delegate) { initStartupLater(context) }
            }?.let { throw IllegalStateException("AsyncStartup initialize later failed: $delegate", it) }
        }
    }

    protected fun destroyServiceBefore(context: Context) {
        val (sync, async) = startupList.partition { it.isSync }
        // 这里必须先倒序排列再反向才能提供正确的析构顺序, 因为 sortedByDescending 排序是稳定的
        for (delegate in async.sortedByDescending { it.priority }.asReversed()) {
            catchingError {
                delegate.destroyStartupBefore(context)
            }?.let { throw IllegalStateException("AsyncStartup destroy before failed: $delegate", it) }
        }
        for (delegate in sync.sortedByDescending { it.priority }.asReversed()) {
            catchingError {
                delegate.destroyStartupBefore(context)
            }?.let { throw IllegalStateException("SyncStartup destroy before failed: $delegate", it) }
        }
    }

    protected fun destroyService(context: Context) {
        val (sync, async) = startupList.partition { it.isSync }
        for (delegate in async.sortedByDescending { it.priority }.asReversed()) {
            catchingError {
                delegate.destroyStartup(context)
            }?.let { throw IllegalStateException("AsyncStartup destroy failed: $delegate", it) }
        }
        for (delegate in sync.sortedByDescending { it.priority }.asReversed()) {
            catchingError {
                delegate.destroyStartup(context)
            }?.let { throw IllegalStateException("SyncStartup destroy failed: $delegate", it) }
        }
        startupList.clear()
    }
}