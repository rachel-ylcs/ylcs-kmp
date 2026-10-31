package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.LocalNativeWindowContext
import androidx.compose.ui.window.NativeWindowContext
import androidx.compose.ui.viewinterop.NativeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun <V : Any, P : Any> NativePlatformViewHost(
    state: BasicPlatformViewState<V, P>,
    parameters: P,
    modifier: Modifier,
    factory: (NativeWindowContext, LifecycleOwner, () -> Unit) -> NativePlatformViewContainer<V>
) {
    val context = LocalNativeWindowContext.current
    val window = context.window
    val owner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    key(state, context, owner, focusManager) {
        val slot = remember { PlatformViewSlot(state) }
        val onNativeFocus = remember {
            {
                focusManager.clearFocus(force = true)
                window.stopTextInput()
            }
        }
        NativeView(
            factory = {
                NativePlatformViewMount(slot).also { mount ->
                    mount.attach(factory(context, owner, onNativeFocus), parameters, onNativeFocus)
                }
            },
            modifier = modifier,
            update = { slot.update(parameters) },
        )
    }
}
