package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.DisposableHandle

internal abstract class BlurNode : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {
    protected var graphicsContext: GraphicsContext? = null
        private set

    internal var coordinates: LayoutCoordinates? = null

    protected var hostStopped: Boolean = false
        private set

    private var lifecycle: Lifecycle? = null
    private var memoryCallback: DisposableHandle? = null

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                hostStopped = true
                releaseResources()
                invalidateDraw()
            }
            Lifecycle.Event.ON_START -> {
                hostStopped = false
                onHostStarted()
                invalidateDraw()
            }
            else -> Unit
        }
    }

    final override val shouldAutoInvalidate: Boolean = false

    final override fun onAttach() {
        hostStopped = false
        onObservedReadsChanged()
        attachBlur()
    }

    final override fun onObservedReadsChanged() {
        observeReads {
            val newContext = currentValueOf(LocalGraphicsContext)
            if (graphicsContext !== newContext) {
                releaseResources()
                graphicsContext = newContext
            }

            val newLifecycle = currentValueOf(LocalLifecycleOwner).lifecycle
            if (lifecycle !== newLifecycle) {
                lifecycle?.removeObserver(lifecycleObserver)
                lifecycle = newLifecycle
                newLifecycle.addObserver(lifecycleObserver)
            }

            memoryCallback?.dispose()
            memoryCallback = registerBlurMemoryCallback(this) {
                releaseResources()
                if (isAttached) invalidateDraw()
            }
        }
        invalidateDraw()
    }

    final override fun onDetach() {
        detachBlur()
        lifecycle?.removeObserver(lifecycleObserver)
        lifecycle = null
        memoryCallback?.dispose()
        memoryCallback = null
        releaseResources()
        graphicsContext = null
        coordinates = null
    }

    final override fun onReset() {
        releaseResources()
        coordinates = null
        resetBlur()
        invalidateDraw()
    }

    protected abstract fun attachBlur()
    protected abstract fun detachBlur()
    protected abstract fun releaseResources()
    protected open fun resetBlur() = Unit
    protected open fun onHostStarted() = Unit
}
