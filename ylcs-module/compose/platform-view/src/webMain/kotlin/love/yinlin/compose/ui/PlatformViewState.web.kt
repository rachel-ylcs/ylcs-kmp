package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.w3c.dom.HTMLElement

@Stable
abstract class PlatformViewState<V : HTMLElement, P : Any> : BasicPlatformViewState<V, P>() {
    protected abstract fun factory(environment: PlatformViewEnvironment): V

    @Composable
    final override fun Render(parameters: P, modifier: Modifier) {
        val owner = LocalLifecycleOwner.current
        key(this, owner) {
            val slot = remember { PlatformViewSlot(this) }
            HtmlElementView(
                modifier = modifier,
                factory = { slot.create(parameters) { factory(PlatformViewEnvironment(owner)) } },
                update = { slot.update(parameters) },
                onReset = null,
                onRelease = { slot.finish() },
            )
        }
    }
}
