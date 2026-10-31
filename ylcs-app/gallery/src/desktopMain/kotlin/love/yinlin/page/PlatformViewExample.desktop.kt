package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.compose.ui.rememberPlatformViewState
import javax.swing.JTextArea

internal actual val platformViewName: String? = "Swing JTextArea"

@Composable
internal actual fun PlatformViewExample(text: String, modifier: Modifier) {
    val state = rememberPlatformViewState { GalleryTextViewState() }
    PlatformViewHost(state, text, modifier)
}

@Stable
private class GalleryTextViewState : PlatformViewState<JTextArea, String>() {
    override fun factory(environment: PlatformViewEnvironment): JTextArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
    }

    override fun synchronize(view: JTextArea, previous: String?, next: String) {
        if (previous != next) view.text = next
    }

    override fun detach(view: JTextArea) {}
}
