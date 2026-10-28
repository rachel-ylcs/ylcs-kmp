package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.extension.mutableRefStateOf

@Stable
abstract class BasicPlatformViewState<V : Any, P : Any> : PlatformViewStateOwner {
    private var active by mutableRefStateOf(false)
    final override val isActive: Boolean get() = active

    private val bindings: MutableSet<PlatformViewBinding<V, P>> = []
    private var activeBinding: PlatformViewBinding<V, P>? = null
    private var releaseRequested = false
    private var resourcesReleased = false

    protected val parameters: P get() = requireNotNull(activeBinding) { "The platform view is not active" }.parameters

    protected open fun attach(view: V) {}
    protected open fun synchronize(view: V, previous: P?, next: P) {}
    protected abstract fun detach(view: V)

    protected open fun releaseResources() {}

    protected fun onView(view: V, action: (V) -> Unit) {
        platformViewOnUi {
            val binding = activeBinding
            if (!releaseRequested && binding != null && binding.active && binding.view === view) action(view)
        }
    }

    protected fun withView(action: (V) -> Unit) {
        platformViewOnUi {
            val binding = activeBinding
            if (!releaseRequested && binding != null && binding.active) action(binding.requireView())
        }
    }

    protected fun launch(view: V, block: suspend CoroutineScope.() -> Unit) {
        platformViewOnUi {
            val binding = activeBinding
            if (!releaseRequested && binding != null && binding.active && binding.view === view) binding.launch(block)
        }
    }

    protected fun cleanup(vararg actions: () -> Unit) = platformViewCleanup(*actions)

    final override fun release() {
        platformViewOnUi {
            releaseRequested = true
            active = false
            platformViewCleanup(*bindings.map { binding -> { binding.cancelTasks() } }.toTypedArray(), ::finishRelease)
        }
    }

    @Composable
    internal abstract fun Render(parameters: P, modifier: Modifier)

    internal fun register(binding: PlatformViewBinding<V, P>) {
        check(!releaseRequested) { "A released platform state cannot create another view" }
        bindings.add(binding)
    }

    internal fun activate(binding: PlatformViewBinding<V, P>) {
        check(!releaseRequested) { "A released platform state cannot attach another host" }
        check(activeBinding == null || activeBinding === binding) { "A platform state cannot be mounted in two places" }
        activeBinding = binding
        binding.active = true
        active = true
        attach(binding.requireView())
    }

    internal fun deactivate(binding: PlatformViewBinding<V, P>) {
        binding.active = false
        if (activeBinding === binding) {
            activeBinding = null
            active = false
        }
    }

    internal fun synchronize(binding: PlatformViewBinding<V, P>, previous: P?, next: P) {
        if (!releaseRequested && activeBinding === binding && binding.active) synchronize(binding.requireView(), previous, next)
    }

    internal fun finish(binding: PlatformViewBinding<V, P>) {
        deactivate(binding)
        platformViewCleanup(binding::cancelTasks, { binding.view?.let(::detach) }, {
            binding.clear()
            bindings.remove(binding)
        }, ::finishRelease)
    }

    private fun finishRelease() {
        if (releaseRequested && bindings.isEmpty() && !resourcesReleased) {
            resourcesReleased = true
            releaseResources()
        }
    }
}
