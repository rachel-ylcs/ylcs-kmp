package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.lifecycle.compose.LocalLifecycleOwner
import platform.UIKit.UIView

@Stable
abstract class PlatformViewState<V : UIView, P : Any> : BasicPlatformViewState<V, P>() {
    protected abstract fun factory(environment: PlatformViewEnvironment): V
    protected open val properties: UIKitInteropProperties = UIKitInteropProperties()

    @Composable
    final override fun Render(parameters: P, modifier: Modifier) {
        val owner = LocalLifecycleOwner.current
        val interopProperties = properties
        key(this, owner, interopProperties) {
            val slot = remember { PlatformViewSlot(this) }
            UIKitView(
                modifier = modifier,
                factory = { slot.create(parameters) { factory(PlatformViewEnvironment(owner)) } },
                update = { slot.update(parameters) },
                onReset = null,
                onRelease = { slot.finish() },
                properties = interopProperties,
            )
        }
    }
}
