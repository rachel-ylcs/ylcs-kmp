package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect

@Composable
fun rememberOffScreenState(): Boolean {
    var value by rememberFalse()
    LifecycleStartEffect(Unit) {
        value = true
        onStopOrDispose {
            value = false
        }
    }
    return value
}

@Composable
inline fun OffScreenEffect(crossinline block: (isForeground: Boolean) -> Unit) {
    LifecycleStartEffect(Unit) {
        block(true)
        onStopOrDispose {
            block(false)
        }
    }
}