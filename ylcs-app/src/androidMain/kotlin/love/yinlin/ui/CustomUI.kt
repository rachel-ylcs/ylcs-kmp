package love.yinlin.ui

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun <T : View> CustomUI(
	view: MutableState<T?>,
	modifier: Modifier = Modifier,
	factory: (Context) -> T,
	update: ((T) -> Unit)? = null,
	reset: ((T) -> Unit)? = null,
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

	AndroidView(
		modifier = modifier,
		factory = { context ->
			view.value ?: factory(context).let {
				view.value = it
				it
			}
		},
		update = { update?.invoke(it) },
		onReset = reset
	)
}