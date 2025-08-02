package love.yinlin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : HTMLElement> CustomUI(
    view: MutableState<T?>,
    modifier: Modifier = Modifier,
    factory: () -> T,
    update: ((T) -> Unit)? = null,
    release: (T, () -> Unit) -> Unit = { _, onRelease -> onRelease() }
) {
    DisposableEffect(view, release) {
        onDispose {
            view.value?.let {
                release(it) {
                    view.value = null
                }
            }
        }
    }

    WebElementView(
        modifier = modifier,
        factory = {
            view.value ?: factory().let {
                view.value = it
                it
            }
        },
        update = { update?.invoke(it) }
    )
}