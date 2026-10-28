package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.awt.Component

@Stable
abstract class PlatformViewState<V : Component, P : Any> : BasicPlatformViewState<V, P>() {
    protected abstract fun factory(environment: PlatformViewEnvironment): V

    @Composable
    final override fun Render(parameters: P, modifier: Modifier) {
        val owner = LocalLifecycleOwner.current
        key(this, owner) {
            val slot = remember { PlatformViewSlot(this) }
            SwingPanel(
                modifier = modifier,
                factory = {
                    val view = slot.create(parameters) {
                        factory(PlatformViewEnvironment(owner))
                    }
                    PlatformSwingContainer(view, slot::finish).also { container ->
                        slot.onForgotten = container::retire
                    }
                },
                update = { slot.update(parameters) },
            )
        }
    }
}
