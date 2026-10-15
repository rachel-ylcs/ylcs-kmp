package love.yinlin.compose.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.text.SimpleEllipsisText

/**
 * [DataTableItem.items] 中的单元格，由派生类保存实际值，表格通过统一接口显示和编辑。
 * [update] 失败时保留原值
 * [captureValue] 可同时捕获多个字段，用于取消编辑。
 */
@Stable
abstract class DataTableCell {
    abstract val text: String
    open val keyboardType: KeyboardType = KeyboardType.Text
    open val editable: Boolean = true

    abstract fun update(value: String): Boolean
    open fun acceptsInput(value: String): Boolean = true
    abstract fun captureValue(): () -> Unit
    open fun toggle(): Boolean = false

    override fun toString(): String = text

    @Composable
    open fun Display(modifier: Modifier) {
        SimpleEllipsisText(
            text = text,
            modifier = modifier.padding(horizontal = Theme.padding.h),
            style = Theme.typography.v8
        )
    }

    @Composable
    open fun Editor(state: DataTableEditState, modifier: Modifier) {
        val focus = LocalFocusManager.current
        val requester = remember(state) { FocusRequester() }
        var hadFocus by rememberFalse(state)
        val transformation = remember(state) {
            InputTransformation {
                val value = asCharSequence().toString()
                if (!state.type.acceptsInput(value)) revertAllChanges()
                else if (changes.changeCount > 0) state.onInput(value)
            }
        }

        Box(
            modifier = modifier.padding(horizontal = Theme.padding.h),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                state = state.rawState,
                inputTransformation = transformation,
                modifier = Modifier.fillMaxWidth().focusRequester(requester).onFocusChanged {
                    if (it.isFocused) hadFocus = true
                    else if (hadFocus) state.commit()
                }.onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.Escape -> {
                            state.cancel()
                            focus.clearFocus()
                            true
                        }
                        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                            state.commit()
                            focus.clearFocus()
                            true
                        }
                        else -> false
                    }
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = Theme.typography.v8.copy(color = Theme.color.onSurface),
                cursorBrush = SolidColor(Theme.color.primary),
                keyboardOptions = KeyboardOptions(keyboardType = state.type.keyboardType, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                onKeyboardAction = remember(state, focus) {
                    KeyboardActionHandler {
                        state.commit()
                        focus.clearFocus()
                    }
                }
            )
        }

        LaunchedEffect(state) { requester.requestFocus() }
    }
}
