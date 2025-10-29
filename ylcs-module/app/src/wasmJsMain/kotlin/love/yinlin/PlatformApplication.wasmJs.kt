package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.extension.Reference

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: Reference<A?>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    @OptIn(ExperimentalComposeUiApi::class)
    fun run() {
        initialize()

        ComposeViewport(viewportContainer = document.body!!) {
            BeginContent()
            Layout()
        }
    }
}