package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import love.yinlin.extension.BaseLazyReference
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.uri.Uri
import love.yinlin.uri.toNSUrl
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController

@Stable
@Suppress("unused")
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>(self, delegate) {
    @Composable
    protected open fun BeginContent() {}

    private val scope = MainScope()

    fun buildUIViewController(): UIViewController {
        val uiViewController = ComposeUIViewController({
            delegate = object : ComposeUIViewControllerDelegate {
                override fun viewDidLoad() {
                    super.viewDidLoad()
                    scope.launch { openServiceLater() }
                }
            }
        }) {
            ComposedLayout {
                BeginContent()
                Content()
            }
        }
        context.controller = uiViewController
        return uiViewController
    }

    fun run() = openService(scope = scope)

    actual fun openUri(uri: Uri): Boolean = catchingDefault(false) {
        val application = UIApplication.sharedApplication
        uri.toNSUrl()?.let { url ->
            application.canOpenURL(url)
            application.openURL(url)
        } ?: false
    }

    actual fun copyText(text: String): Boolean {
        UIPasteboard.generalPasteboard.setString(text)
        return true
    }
}