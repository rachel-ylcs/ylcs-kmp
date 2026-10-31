package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.compose.ui.rememberPlatformViewState
import platform.AppKit.NSTextField

internal actual val platformViewName: String? = "AppKit NSTextField"

@Composable
internal actual fun PlatformViewExample(text: String, modifier: Modifier) {
    val state = rememberPlatformViewState { GalleryTextViewState() }
    PlatformViewHost(state, text, modifier)
}

@Stable
private class GalleryTextViewState : PlatformViewState<NSTextField, String>() {
    override fun factory(environment: PlatformViewEnvironment): NSTextField = NSTextField().apply {
        editable = true
        selectable = true
        bezeled = true
    }

    override fun synchronize(view: NSTextField, previous: String?, next: String) {
        if (previous != next) view.stringValue = next
    }

    override fun detach(view: NSTextField) {}
}
