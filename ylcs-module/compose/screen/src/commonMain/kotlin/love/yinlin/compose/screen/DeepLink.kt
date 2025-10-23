package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import love.yinlin.common.uri.Uri

@Stable
fun interface DeepLink {
    fun process(manager: ScreenManager, uri: Uri)

    @Stable
    companion object {
        val DEFAULT = DeepLink { manager, uri -> }

        private var cached: Uri? = null
        private var listener: ((uri: Uri) -> Unit)? = null
            set(value) {
                field = value
                if (value != null) {
                    cached?.let { value.invoke(it) }
                    cached = null
                }
            }

        @Composable
        fun Register(deeplink: DeepLink, manager: ScreenManager) {
            DisposableEffect(deeplink, manager) {
                listener = { deeplink.process(manager, it) }
                onDispose { listener = null }
            }
        }

        fun openUri(uri: Uri) {
            cached = uri
            listener?.let {
                it.invoke(uri)
                cached = null
            }
        }
    }
}