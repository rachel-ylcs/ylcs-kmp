package love.yinlin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalDensity
import love.yinlin.Colors
import love.yinlin.DesktopContext
import love.yinlin.app
import love.yinlin.extension.Reference
import java.awt.Component

@Composable
fun <T : Component> CustomUI(
	view: Reference<T>,
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

	(app as DesktopContext).rawDensity?.let { density ->
		CompositionLocalProvider(LocalDensity provides density) {
			SwingPanel(
				modifier = modifier,
				background = Colors.Transparent,
				factory = {
					val factoryView = view.value ?: factory()
					view.value = factoryView
					factoryView
				},
				update = { update?.invoke(it) }
			)
		}
	}
}