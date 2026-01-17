package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import java.awt.Component

@Stable
abstract class PlatformView<V : Component> : BasicPlatformView<V>() {
    protected abstract fun build(): V

    @Composable
    override fun HostView(modifier: Modifier) {
        SwingPanel(
            background = Color.Transparent,
            factory = { hostFactory(::build) },
            update = hostUpdate,
            modifier = modifier
        )

        DisposableEffect(Unit) {
            onDispose {
                host(::hostRelease)
            }
        }
    }
}