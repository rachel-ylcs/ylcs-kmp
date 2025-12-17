package love.yinlin.platform

import android.content.Context
import android.view.View
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

@Stable
abstract class PlatformView<T : View> {
    protected abstract fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): T
    protected open fun release(view: T) { }
    protected open fun update(view: T) { }
    protected open fun reset(view: T) { }

    protected var view: T? = null
    private val lock = SynchronizedObject()

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val activityRegistry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
        val lifecycleOwner = LocalLifecycleOwner.current

        AndroidView(
            modifier = modifier,
            factory = { context ->
                synchronized(lock) {
                    view ?: build(context, lifecycleOwner, activityRegistry).also { view = it }
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