package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import love.yinlin.coroutines.Coroutines

internal class ApplicationWindowManager(private val application: Application<*>) {
    private val windows = mutableStateListOf<ApplicationWindow>()
    private var closed = false

    val isEmpty: Boolean get() = windows.isEmpty()

    fun open(window: ApplicationWindow) = Coroutines.dispatchUI {
        if (!closed && window !in windows) {
            val registered = window.register {
                windows.remove(window)
                if (!window.isActive) window.release()
            }
            if (registered) windows.add(window)
        }
    }

    fun close() {
        closed = true
        for (window in windows.toList()) if (!window.isActive) window.release()
        windows.clear()
    }

    @Composable
    fun Content() {
        for (window in windows.toList()) key(window) {
            DisposableEffect(window) {
                try { window.activate() }
                catch (failure: Throwable) {
                    try { window.release() }
                    catch (cleanup: Throwable) { if (cleanup !== failure) failure.addSuppressed(cleanup) }
                    throw failure
                }
                onDispose { window.release() }
            }
            window.Window { window.ComposedContent(application) }
        }
    }
}
