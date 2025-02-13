package love.yinlin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import love.yinlin.extension.Reference
import platform.UIKit.UIView

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : UIView> CustomUI(
	view: Reference<T>,
	modifier: Modifier = Modifier,
	factory: () -> T,
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

	UIKitView(
		modifier = modifier,
		properties = UIKitInteropProperties(
			interactionMode = UIKitInteropInteractionMode.NonCooperative
		),
		factory = {
			val factoryView = view.value ?: factory()
			view.value = factoryView
			factoryView
		},
		update = { update?.invoke(it) },
		onReset = reset
	)
}