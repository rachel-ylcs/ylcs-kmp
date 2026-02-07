package love.yinlin.compose.ui

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

@Stable
abstract class PlatformView<V : View> : BasicPlatformView<V>() {
    protected abstract fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): V

    @Composable
    final override fun HostView(modifier: Modifier) {
        val activityRegistry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
        val lifecycleOwner = LocalLifecycleOwner.current
        AndroidView(
            modifier = modifier,
            factory = { hostFactory { build(it, lifecycleOwner, activityRegistry) } },
            update = hostUpdate,
            onReset = hostReset,
            onRelease = ::hostRelease
        )
    }
}