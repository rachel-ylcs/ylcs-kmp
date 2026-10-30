package love.yinlin.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.mainImmediateContext

/**
 * 一个独立窗口的生命周期。每次打开使用新实例，持久业务状态可由调用方另行持有。
 * [Window] 只创建一个平台窗口，并将系统关闭请求交给 [close]；[Content] 自动使用应用的主题和资源配置。
 * 窗口离开组合后取消其协程并调用 [uninitialize]，关闭子窗口不会退出应用。
 */
@Stable
abstract class ApplicationWindow {
    protected open val background: Color? = null
    protected open val contentModifier: Modifier = Modifier.fillMaxSize()
    private var opened = false
    private var closed = false
    private var requestClose: (() -> Unit)? = null
    private var windowScope: CoroutineScope? = null

    internal val isClosed: Boolean get() = closed

    var isActive by mutableStateOf(false)
        private set

    val scope: CoroutineScope get() = checkNotNull(windowScope) { "Window content is not active" }

    @Composable
    abstract fun Window(content: @Composable () -> Unit)

    @Composable
    abstract fun Content()

    protected open fun initialize() {}

    protected open fun uninitialize() {}

    fun close() = Coroutines.dispatchUI {
        closed = true
        requestClose?.invoke()
    }

    internal fun register(close: () -> Unit): Boolean {
        if (closed) return false
        check(!opened) { "Use a new ApplicationWindow instance for each opening" }
        opened = true
        requestClose = close
        return true
    }

    internal fun activate() {
        windowScope = CoroutineScope(SupervisorJob() + mainImmediateContext)
        isActive = true
        initialize()
    }

    internal fun release() {
        closed = true
        requestClose = null
        val scope = windowScope ?: return
        isActive = false
        try { scope.cancel() }
        finally {
            try { uninitialize() }
            finally { windowScope = null }
        }
    }

    @Composable
    internal fun ComposedContent(application: Application<*>) {
        application.ComposedLayout(modifier = contentModifier, bgColor = background) { Content() }
    }
}
