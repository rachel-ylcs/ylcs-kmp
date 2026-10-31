package love.yinlin.compose.ui

import androidx.compose.runtime.RememberObserver

internal class PlatformViewSlot<V : Any, P : Any>(private val state: BasicPlatformViewState<V, P>) : RememberObserver {
    private var binding: PlatformViewBinding<V, P>? = null
    private var remembered = false
    var onForgotten: (() -> Unit)? = null

    fun create(parameters: P, factory: () -> V): V {
        check(binding == null) { "The platform container created a view twice" }
        val created = PlatformViewBinding(state, parameters)
        binding = created
        val view = created.create(factory)
        if (remembered) created.activate()
        return view
    }

    fun update(parameters: P) = binding?.update(parameters)

    fun finish() {
        try { binding?.finish() }
        finally {
            binding = null
            onForgotten = null
        }
    }

    override fun onRemembered() {
        remembered = true
        binding?.activate()
    }

    override fun onForgotten() {
        remembered = false
        platformViewCleanup(
            { binding?.deactivate() },
            { onForgotten?.invoke() }
        )
    }

    override fun onAbandoned() = finish()
}
