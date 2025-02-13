package love.yinlin.ui

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import love.yinlin.extension.Reference

@Composable
fun <T : View> CustomUI(
	view: Reference<T>,
	modifier: Modifier = Modifier,
	factory: (Context) -> T,
	update: ((T) -> Unit)? = null,
	reset: ((T) -> Unit)? = null,
	release: ((T) -> Unit)? = null
) {
	DisposableEffect(Unit) {
		onDispose {
			view.value?.let {
				release?.invoke(it)
				view.value = null
			}
		}
	}

	AndroidView(
		modifier = modifier,
		factory = {
			val factoryView = view.value ?: factory(it)
			view.value = factoryView
			factoryView
		},
		update = { update?.invoke(it) },
		onReset = reset
	)
}