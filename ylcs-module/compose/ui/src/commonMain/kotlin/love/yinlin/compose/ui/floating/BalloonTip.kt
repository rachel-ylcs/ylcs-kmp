package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.text.Text

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun BalloonTip(text: String, content: @Composable () -> Unit) {
    var visible by rememberFalse()

    Flyout(
        visible = visible,
        onClickOutside = { visible = false },
        focusable = false,
        flyout = {
            Surface(
                contentPadding = Theme.padding.value,
                shape = Theme.shape.v5,
                shadowElevation = Theme.shadow.v4,
            ) {
                Text(text = text)
            }
        }
    ) {
        Box(modifier = Modifier.pointerInput(Unit) {
            coroutineScope {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes[0].type == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter -> visible = true
                                PointerEventType.Exit -> visible = false
                            }
                        }
                    }
                }
            }
        }) {
            content()
        }
    }
}

@Composable
@NonRestartableComposable
fun BalloonTip(text: String, enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled && text.isNotEmpty()) BalloonTip(text, content)
    else content()
}