package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.compose.ui.rememberPlatformViewState
import love.yinlin.extension.createElement
import org.w3c.dom.HTMLTextAreaElement

internal actual val platformViewName: String? = "HTML textarea"

@Composable
internal actual fun PlatformViewExample(text: String, modifier: Modifier) {
    val state = rememberPlatformViewState { GalleryTextViewState() }
    PlatformViewHost(state, text, modifier)
}

@Stable
private class GalleryTextViewState : PlatformViewState<HTMLTextAreaElement, String>() {
    override fun factory(environment: PlatformViewEnvironment): HTMLTextAreaElement = createElement {
        style.width = "100%"
        style.height = "100%"
        style.boxSizing = "border-box"
        style.resize = "none"
    }

    override fun synchronize(view: HTMLTextAreaElement, previous: String?, next: String) {
        if (previous != next) view.value = next
    }

    override fun detach(view: HTMLTextAreaElement) {}
}
