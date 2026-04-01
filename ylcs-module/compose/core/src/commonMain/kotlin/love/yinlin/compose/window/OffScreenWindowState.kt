package love.yinlin.compose.window

import androidx.compose.runtime.*
import androidx.lifecycle.compose.LifecycleStartEffect
import love.yinlin.compose.extension.rememberFalse

/**
 * 当前屏幕或窗口是否处于离屏状态
 */
@Composable
fun rememberOffScreenWindowState(): State<Boolean> {
    val lastValue = rememberFalse()
    LifecycleStartEffect(Unit) {
        lastValue.value = true
        onStopOrDispose {
            lastValue.value = false
        }
    }

    return lastValue
}

/**
 * 当前屏幕或窗口是否处于离屏状态
 */
@Composable
fun OffScreenWindowEffect(block: (isForeground: Boolean) -> Unit) {
    var lastValue by rememberFalse()
    LifecycleStartEffect(block) {
        if (!lastValue) {
            lastValue = true
            block(true)
        }
        onStopOrDispose {
            if (lastValue) {
                lastValue = false
                block(false)
            }
        }
    }
}