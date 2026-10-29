package love.yinlin.compose

import android.provider.Settings
import androidx.compose.runtime.*
import love.yinlin.compose.window.FloatingView
import love.yinlin.extension.catchingError

/**
 * 创建应用管理的 Android 悬浮窗，自动请求悬浮窗权限并在窗口关闭时注销请求、释放 View。
 * [layoutInScreen]、[touchable] 为创建参数；[onOpen] 可取得底层宿主以更新位置。
 * 无前台 Activity 时等待其就绪，Activity 重建后重新处理待打开请求；明确拒绝权限时调用 [onPermissionFailed]。
 */
@Composable
fun ApplicationWindow.OverlayWindow(
    layoutInScreen: Boolean = true,
    touchable: Boolean = true,
    onPermissionFailed: () -> Unit = {},
    onOpen: (FloatingView) -> Unit = {},
    onClose: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val host = LocalAndroidApplicationWindowHost.current
    val currentContent by rememberUpdatedState(content)
    val currentOpen by rememberUpdatedState(onOpen)
    val currentClose by rememberUpdatedState(onClose)
    val currentPermissionFailed by rememberUpdatedState(onPermissionFailed)
    var permitted by remember { mutableStateOf(Settings.canDrawOverlays(host.application.rawContext)) }
    val activity = host.application.activity

    DisposableEffect(this, host, activity) {
        var request: OverlayPermissionRequest? = null
        if (!permitted && !isClosed && !host.isClosed) {
            if (activity != null) {
                request = OverlayPermissionRequest(activity) { granted ->
                    if (!isClosed && !host.isClosed) {
                        if (granted == true) permitted = true
                        else if (granted == false) {
                            close()
                            currentPermissionFailed()
                        }
                    }
                }
                request.launch()
            }
        }
        onDispose { request?.close() }
    }

    if (permitted) DisposableEffect(this, host) {
        val initialLayoutInScreen = layoutInScreen
        val initialTouchable = touchable
        val view = object : FloatingView() {
            override val layoutInScreen = initialLayoutInScreen
            override val touchable = initialTouchable

            override fun onAttached() { currentOpen(this) }

            override fun onDetached() {
                try { currentClose() }
                finally { this@OverlayWindow.close() }
            }

            @Composable
            override fun Content() { currentContent() }
        }
        if (!isClosed && !host.isClosed) {
            catchingError { view.attach(host.application.rawContext) }?.let { failure ->
                close()
                if (failure is SecurityException || !Settings.canDrawOverlays(host.application.rawContext)) currentPermissionFailed()
                else throw failure
            }
        }
        onDispose { view.detach() }
    }
}
