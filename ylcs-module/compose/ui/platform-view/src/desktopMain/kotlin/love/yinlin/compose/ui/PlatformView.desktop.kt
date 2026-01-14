package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import java.awt.Component

@Stable
abstract class PlatformView<T : Component> {
    protected abstract fun build(): T
    protected open fun release(view: T) { }
    protected open fun update(view: T) { }

    private var view: T? = null
    private val lock = SynchronizedObject()

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        DisposableEffect(Unit) {
            onDispose {
                synchronized(lock) {
                    view?.let { release(it) }
                    view = null
                }
            }
        }

        SwingPanel(
            background = Color.Transparent,
            factory = {
                synchronized(lock) {
                    view ?: build().also { view = it }
                }
            },
            update = ::update,
            modifier = modifier
        )
    }
}