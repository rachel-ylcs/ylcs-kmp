package love.yinlin.compose.ui.floating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import love.yinlin.compose.Theme

@Composable
fun Flyout(
    visible: Boolean,
    onClickOutside: () -> Unit ,
    position: FlyoutPosition = FlyoutPosition.Top,
    space: Dp = Theme.padding.v,
    flyout: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box {
        val spacePx = with(LocalDensity.current) { space.roundToPx() }
        val containerSize = LocalWindowInfo.current.containerSize
        val positionProvider = remember(position, spacePx, containerSize) {
            FlyoutPositionProvider(position, spacePx, containerSize)
        }

        val transition = updateTransition(visible)
        if (transition.currentState || transition.targetState) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = onClickOutside,
                properties = PopupProperties(focusable = false, clippingEnabled = false)
            ) {
                val duration = Theme.animation.duration.v8
                val spec = tween<Float>(duration)

                transition.AnimatedVisibility(
                    visible = { it },
                    enter = scaleIn(spec) + fadeIn(spec),
                    exit = scaleOut(spec) + fadeOut(spec)
                ) {
                    flyout()
                }
            }
        }

        content()
    }
}