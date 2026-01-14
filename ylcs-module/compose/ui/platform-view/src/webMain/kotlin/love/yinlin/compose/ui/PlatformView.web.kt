package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.w3c.dom.HTMLElement

@Stable
abstract class PlatformView<T : HTMLElement> {
    protected abstract fun build(): T
    protected open fun release(view: T) { }
    protected open fun update(view: T) { }
    protected open fun reset(view: T) { }

    protected var view: T? = null
    private val lock = SynchronizedObject()

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        WebElementView(
            modifier = modifier,
            factory = {
                synchronized(lock) {
                    view ?: build().also { view = it }
                }
            },
            update = ::update,
            onReset = if (this is ResetPlatformView) ::reset else null,
            onRelease = {
                synchronized(lock) {
                    release(it)
                    view = null
                }
            },
        )
    }
}