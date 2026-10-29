package love.yinlin.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import love.yinlin.extension.cleaning

internal val LocalAndroidApplicationWindowHost = staticCompositionLocalOf<AndroidApplicationWindowHost> { error("OverlayWindow requires an application window host") }

/** 独立于 Activity 的窗口组合；上层 Owner 销毁时同步释放所有窗口。 */
internal class AndroidApplicationWindowHost(
    val application: PlatformApplication<*>,
    private val owner: LifecycleOwner,
    private val onClosed: () -> Unit,
) : LifecycleEventObserver {
    private val windows = ApplicationWindowManager(application)
    private val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
    private val changes = Channel<Unit>(Channel.CONFLATED)
    private val snapshotObserver = Snapshot.registerGlobalWriteObserver { changes.trySend(Unit) }
    private val recomposer = Recomposer(scope.coroutineContext)
    private val composition = Composition(object : AbstractApplier<Unit>(Unit) {
        override fun onClear() = Unit
        override fun insertTopDown(index: Int, instance: Unit) = error("ApplicationWindow.Window must create an overlay rather than UI nodes")
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun remove(index: Int, count: Int) = Unit
    }, recomposer)
    var isClosed = false
        private set

    init {
        owner.lifecycle.addObserver(this)
        scope.launch { for (change in changes) Snapshot.sendApplyNotifications() }
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        scope.launch {
            snapshotFlow { windows.isEmpty }.first { it }
            close()
        }
        try {
            composition.setContent {
                CompositionLocalProvider(LocalAndroidApplicationWindowHost provides this) { windows.Content() }
            }
        }
        catch (failure: Throwable) {
            try { close() }
            catch (cleanup: Throwable) { if (cleanup !== failure) failure.addSuppressed(cleanup) }
            throw failure
        }
    }

    fun open(window: ApplicationWindow) {
        if (!isClosed) windows.open(window)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) close()
    }

    private fun close() {
        if (isClosed) return
        isClosed = true
        owner.lifecycle.removeObserver(this)
        cleaning({ windows.close() }) {
            cleaning({ composition.dispose() }) {
                cleaning({ recomposer.cancel() }) {
                    snapshotObserver.dispose()
                    changes.close()
                    scope.cancel()
                    onClosed()
                }
            }
        }
    }
}
