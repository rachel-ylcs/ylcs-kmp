package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.input.PlatformImeOptions
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIAction
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIToolbar
import platform.UIKit.UIViewAutoresizingFlexibleWidth

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
internal fun IOSKeyboardDismissal(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val interceptor = remember(focusManager) {
        val action = UIAction.actionWithTitle("收起键盘", image = null, identifier = null) {
            focusManager.clearFocus(force = true)
            currentOnDismiss.value()
        }
        val toolbar = UIToolbar().apply {
            setItems([
                UIBarButtonItem.flexibleSpaceItem(),
                UIBarButtonItem(primaryAction = action, menu = null),
            ], animated = false)
            sizeToFit()
            autoresizingMask = UIViewAutoresizingFlexibleWidth
        }
        val options = PlatformImeOptions { inputAccessoryView(toolbar) }

        PlatformTextInputInterceptor { request, nextHandler ->
            val originalOptions = request.imeOptions
            // 已有平台键盘配置的输入框继续使用自己的设置。
            val configuredRequest = if (originalOptions.platformImeOptions == null) {
                object : PlatformTextInputMethodRequest by request {
                    override val imeOptions = originalOptions.copy(platformImeOptions = options, hintLocales = originalOptions.hintLocales)
                }
            }
            else request
            nextHandler.startInputMethod(configuredRequest)
        }
    }
    InterceptPlatformTextInput(interceptor, content)
}
