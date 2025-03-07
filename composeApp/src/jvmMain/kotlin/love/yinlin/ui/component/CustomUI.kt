package love.yinlin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalDensity
import love.yinlin.common.Colors
import love.yinlin.platform.appNative
import java.awt.Component

@Composable
fun <T : Component> CustomUI(
	view: MutableState<T?>,
	modifier: Modifier = Modifier,
	factory: () -> T,
	update: ((T) -> Unit)? = null,
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

	appNative.rawDensity?.let { density ->
		CompositionLocalProvider(LocalDensity provides density) {
			SwingPanel(
				modifier = modifier,
				background = Colors.Transparent,
				factory = {
					view.value ?: factory().let {
						view.value = it
						it
					}
				},
				update = { update?.invoke(it) }
			)
		}
	}
}