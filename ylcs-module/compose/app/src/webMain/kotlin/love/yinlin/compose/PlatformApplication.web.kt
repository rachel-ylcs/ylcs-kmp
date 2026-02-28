package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    protected open val enableAccessibility: Boolean = true

    @OptIn(ExperimentalComposeUiApi::class)
    fun run() {
        val mainScope = MainScope()
        openService(scope = mainScope, later = false, immediate = true)

        // 有待考证
        window.onclose = {
            closeService(before = true, immediate = true)
            mainScope.cancel()
        }

        ComposeViewport(
            viewportContainer = document.body!!,
            configure = {
                isA11YEnabled = enableAccessibility
            }
        ) {
            ComposedLayout {
                BeginContent()
                Content()
            }
        }
    }
}