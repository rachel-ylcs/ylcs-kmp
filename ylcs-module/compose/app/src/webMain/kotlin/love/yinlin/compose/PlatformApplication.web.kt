package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    @OptIn(ExperimentalComposeUiApi::class)
    fun run() {
        openService(later = false, immediate = true)

        // 有待考证
        window.onclose = {
            closeService(before = true, immediate = true)
        }

        ComposeViewport(viewportContainer = document.body!!) {
            Layout {
                BeginContent()
                Content()
            }
        }
    }
}