package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import love.yinlin.extension.then
import love.yinlin.uri.Uri

/**
 * 深度链接
 *
 * 全局只能绑定维护一个 [ScreenManager] 实例
 */
@Stable
fun interface DeepLink {
    fun onDeepLink(manager: ScreenManager, uri: Uri)

    @Stable
    companion object {
        private var cached: Uri? = null
        private var listener: ((uri: Uri) -> Unit)? = null
            set(value) {
                field = value
                if (value != null) {
                    cached?.then(value)
                    cached = null
                }
            }

        @Composable
        fun Register(deeplink: DeepLink?, manager: ScreenManager) {
            DisposableEffect(deeplink, manager) {
                listener = if (deeplink == null) null else {
                    { deeplink.onDeepLink(manager, it) }
                }
                onDispose {
                    listener = null
                }
            }
        }

        fun navigate(uri: Uri) {
            cached = uri
            val currentListener = listener
            if (currentListener != null) {
                currentListener(uri)
                cached = null
            }
        }
    }
}