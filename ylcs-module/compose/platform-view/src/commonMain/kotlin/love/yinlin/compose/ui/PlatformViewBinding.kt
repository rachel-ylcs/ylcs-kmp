package love.yinlin.compose.ui

import kotlinx.coroutines.*
import love.yinlin.coroutines.mainImmediateContext

internal class PlatformViewBinding<V : Any, P : Any>(private val state: BasicPlatformViewState<V, P>, parameters: P) {
    var view: V? = null
        private set
    var active = false
    private var latest: P? = parameters
    private var applied: P? = null
    private var hasUpdate = false
    private var closed = false
    private var tasks: CoroutineScope? = null

    val parameters: P get() = requireNotNull(latest) { "The platform binding has been released" }
    fun requireView(): V = requireNotNull(view) { "The platform view has not been created" }

    fun create(factory: () -> V): V {
        state.register(this)
        try {
            return factory().also { view = it }
        }
        catch (failure: Throwable) {
            try { finish() }
            catch (cleanup: Throwable) {
                if (cleanup !== failure) failure.addSuppressed(cleanup)
            }
            throw failure
        }
    }

    fun activate() {
        if (closed || active) return
        try {
            state.activate(this)
            applyParameters()
        }
        catch (failure: Throwable) {
            try { finish() }
            catch (cleanup: Throwable) {
                if (cleanup !== failure) failure.addSuppressed(cleanup)
            }
            throw failure
        }
    }

    fun update(parameters: P) {
        if (closed) return
        latest = parameters
        hasUpdate = true
        applyParameters()
    }

    private fun applyParameters() {
        if (active && hasUpdate && applied != parameters) {
            val next = parameters
            state.synchronize(this, applied, next)
            applied = next
        }
    }

    fun deactivate() {
        if (!closed) {
            state.deactivate(this)
            cancelTasks()
        }
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        val scope = tasks ?: CoroutineScope(SupervisorJob() + mainImmediateContext).also { tasks = it }
        scope.launch(block = block)
    }

    fun cancelTasks() {
        val scope = tasks
        tasks = null
        scope?.cancel()
    }

    fun finish() {
        if (closed) return
        closed = true
        state.finish(this)
    }

    fun clear() {
        view = null
        latest = null
        applied = null
    }
}
