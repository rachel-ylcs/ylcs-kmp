package love.yinlin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import love.yinlin.common.Colors
import java.awt.Component

@Composable
fun <T : Component> CustomUI(
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

	SwingPanel(
		background = Colors.Transparent,
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