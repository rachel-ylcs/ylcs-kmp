package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.extension.LazyReference

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: LazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    @OptIn(ExperimentalComposeUiApi::class)
    fun run() {
        initialize(delay = false)

        initialize(delay = true)

        // 有待考证
//        window.onclose = {
//            destroy(delay = true)
//
//            destroy(delay = false)
//        }

        ComposeViewport(viewportContainer = document.body!!) {
            Layout {
                BeginContent()
                Content()
            }
        }
    }
}