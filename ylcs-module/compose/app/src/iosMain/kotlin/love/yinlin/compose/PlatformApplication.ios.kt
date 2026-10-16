package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.MainScope
import love.yinlin.extension.BaseLazyReference
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.SandboxUri
import love.yinlin.uri.Uri
import love.yinlin.uri.toNSUrl
import platform.UIKit.*

@Stable
@Suppress("unused")
actual abstract class PlatformApplication<out A : PlatformApplication<A>> actual constructor(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A>(self, context) {
    constructor(self: BaseLazyReference<A>): this(self, PlatformContext.Instance)

    @Composable
    protected open fun BeginContent() {}

    private val scope = MainScope()

    fun buildUIViewController(): UIViewController = object : CMPUIViewController() {
        @Composable
        override fun ComposeUI() {
            ComposedLayout {
                BeginContent()
                Content()
            }
        }

        override fun onViewLoad() {
            initPoolLater(scope)
        }
    }.also { controller = it }

    fun run() = initApplication(scope = scope)

    actual fun backHome() { }

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

    actual fun implicitFileUri(uri: Uri): ImplicitUri = SandboxUri(uri.toNSUrl()!!)
}
