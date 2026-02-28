package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate
import platform.UIKit.UIViewController

@Stable
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    private val scope = MainScope()

    fun buildUIViewController(): UIViewController {
        val uiViewController = ComposeUIViewController {
            ComposedLayout {
                BeginContent()
                Content()
            }
        }
        context.controller = uiViewController
        openService(scope = scope, later = true, immediate = false)
        return uiViewController
    }

    fun run() = openService(scope = scope, later = false, immediate = false)
}