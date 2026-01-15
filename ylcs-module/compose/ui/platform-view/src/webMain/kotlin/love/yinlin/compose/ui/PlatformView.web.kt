package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import org.w3c.dom.HTMLElement

@Stable
abstract class PlatformView<V : HTMLElement> : BasicPlatformView<V>() {
    protected abstract fun build(): V

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun HostView(modifier: Modifier) {
        WebElementView(
            modifier = modifier,
            factory = { hostFactory(::build) },
            update = hostUpdate,
            onReset = hostReset,
            onRelease = ::hostRelease
        )
    }
}