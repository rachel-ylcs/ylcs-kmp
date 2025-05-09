package love.yinlin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIView

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : UIView> CustomUI(
    view: MutableState<T?>,
    modifier: Modifier = Modifier,
    factory: () -> T,
    update: ((T) -> Unit)? = null,
    reset: ((T) -> Unit)? = null,
    release: (T, () -> Unit) -> Unit = { _, onRelease -> onRelease() }
) {
    DisposableEffect(Unit) {
        onDispose {
            view.value?.let {
                release(it) {
                    view.value = null
                }
            }
        }
    }

    UIKitView(
        modifier = modifier,
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative
        ),
        factory = {
            view.value ?: factory().let {
                view.value = it
                it
            }
        },
        update = { update?.invoke(it) },
        onReset = reset
    )
}