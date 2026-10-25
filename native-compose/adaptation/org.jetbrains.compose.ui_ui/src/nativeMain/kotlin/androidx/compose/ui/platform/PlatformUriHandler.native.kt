package androidx.compose.ui.platform

import org.jetbrains.skiko.window.NativeDesktop

internal actual fun createPlatformUriHandler(): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        require(NativeDesktop.openUri(uri)) { "No platform handler could open URI: $uri" }
    }
}
