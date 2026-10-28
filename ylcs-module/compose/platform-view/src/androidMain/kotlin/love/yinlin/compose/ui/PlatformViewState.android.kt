package love.yinlin.compose.ui

import android.view.View
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Stable
abstract class PlatformViewState<V : View, P : Any> : BasicPlatformViewState<V, P>() {
    protected abstract fun factory(environment: PlatformViewEnvironment): V

    @Composable
    final override fun Render(parameters: P, modifier: Modifier) {
        val context = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
        key(this, context, owner, registry) {
            val slot = remember { PlatformViewSlot(this) }
            AndroidView(
                modifier = modifier,
                factory = { nativeContext ->
                    slot.create(parameters) {
                        factory(PlatformViewEnvironment(nativeContext, owner, registry))
                    }
                },
                update = { slot.update(parameters) },
                onReset = null,
                onRelease = { slot.finish() },
            )
        }
    }
}
