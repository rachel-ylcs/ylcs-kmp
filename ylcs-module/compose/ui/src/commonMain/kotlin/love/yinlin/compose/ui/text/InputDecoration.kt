package love.yinlin.compose.ui.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.CancellationException
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Ripple
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.extension.catchingError

/**
 * 输入框装饰
 *
 * 可以为输入框提供自定义的前导或后导装饰组件
 * 提供如图标、文本、长度查看器等默认装饰器
 */
@Stable
interface InputDecoration {
    val alignment: Alignment.Vertical
    @Composable
    fun Content(state: InputState)

    /**
     * 图标装饰
     * @param icon 图标
     * @param visible 是否可视 (不可视仍然会占据等量体积)
     * @param enabled 是否允许点击
     * @param onClick 点击事件
     */
    @Stable
    class Icon(
        private val icon: (InputState) -> ImageVector,
        private val visible: (InputState) -> Boolean = { true },
        private val enabled: (InputState) -> Boolean = { true },
        private val onClick: ((InputState) -> Unit)? = null
    ) : InputDecoration {
        override val alignment: Alignment.Vertical = Alignment.CenterVertically

        @Composable
        override fun Content(state: InputState) {
            if (visible(state)) {
                love.yinlin.compose.ui.image.Icon(
                    icon = icon(state),
                    enabled = enabled(state),
                    onClick = onClick?.let { { it(state) } }
                )
            } else {
                Layout(modifier = Modifier.size(Theme.size.icon), MeasurePolicies.Empty)
            }
        }

        companion object {
            val Clear = Icon(
                icon = { Icons.Clear },
                visible = { !it.isEmpty },
                onClick = { it.clear() }
            )
        }
    }

    /**
     * 长度查看器装饰
     */
    @Stable
    object LengthViewer : InputDecoration {
        override val alignment: Alignment.Vertical = Alignment.CenterVertically

        @Composable
        override fun Content(state: InputState) {
            val text by rememberDerivedState {
                val maxLength = if (state.maxLength == Int.MAX_VALUE) "?" else state.maxLength.toString()
                "${state.text.length}/$maxLength"
            }

            SimpleEllipsisText(
                text = text,
                color = if (state.isFull) Theme.color.warning else LocalColor.current,
                style = LocalStyle.current.bold
            )
        }
    }

    @Stable
    object PasswordViewer : InputDecoration {
        override val alignment: Alignment.Vertical = Alignment.CenterVertically

        @Composable
        override fun Content(state: InputState) {
            if (!state.isEmpty) {
                val interactionSource = remember { MutableInteractionSource() }

                var isPressed by rememberFalse()
                val oldTransformation = remember { state.visualTransformation }

                LaunchedEffect(isPressed) {
                    state.visualTransformation = if (isPressed) VisualTransformation.None else oldTransformation
                }

                DisposableEffect(Unit) {
                    onDispose {
                        state.visualTransformation = oldTransformation
                    }
                }

                love.yinlin.compose.ui.image.Icon(
                    modifier = Modifier.hoverable(interactionSource)
                        .indication(interactionSource, Ripple)
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = { offset ->
                                val press = PressInteraction.Press(offset)
                                interactionSource.emit(press)
                                isPressed = true
                                val err = catchingError { awaitRelease() }
                                isPressed = false
                                interactionSource.emit(if (err is CancellationException) PressInteraction.Cancel(press) else PressInteraction.Release(press))
                            })
                        },
                    icon = if (isPressed) Icons.VisibilityOff else Icons.Visibility,
                )
            }
            else {
                Layout(modifier = Modifier.size(Theme.size.icon), MeasurePolicies.Empty)
            }
        }
    }
}