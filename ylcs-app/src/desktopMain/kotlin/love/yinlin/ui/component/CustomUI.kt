package love.yinlin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import love.yinlin.common.Colors
import love.yinlin.platform.appNative
import java.awt.Component

@Composable
fun <T : Component> CustomUI(
	view: MutableState<T?>,
	modifier: Modifier = Modifier,
	factory: () -> T,
	update: ((T) -> Unit)? = null,
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

    CompositionLocalProvider(LocalDensity provides Density(appNative.rawDensity)) {
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
}