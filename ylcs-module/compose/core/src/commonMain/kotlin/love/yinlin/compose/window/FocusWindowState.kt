package love.yinlin.compose.window

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import love.yinlin.compose.extension.rememberFalse

/**
 * 当前屏幕或窗口是否处于焦点状态
 */
@Composable
fun rememberFocusWindowState(): State<Boolean> {
    val lastValue = rememberFalse()
    val observer = remember {
        LifecycleEventObserver { source, _ ->
            lastValue.value = source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(observer) {
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return lastValue
}

/**
 * 当前屏幕或窗口是否处于焦点状态
 */
@Composable
fun FocusWindowEffect(block: (isFocus: Boolean) -> Unit) {
    var lastValue: Boolean by rememberFalse()
    val observer = remember(block) {
        LifecycleEventObserver { source, _ ->
            val newValue = source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (newValue != lastValue) {
                lastValue = newValue
                block(newValue)
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(observer) {
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}