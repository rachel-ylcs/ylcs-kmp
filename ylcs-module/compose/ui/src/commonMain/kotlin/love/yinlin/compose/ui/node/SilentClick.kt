package love.yinlin.compose.ui.node

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * 静默点击
 *
 * 去除点击的水波纹, 但不影响其他事件消费
 *
 * 注意不能通过将clickable的indicator设置null实现，
 * 这样会拦截长按事件导致例如内部文本无法被鼠标选择等问题
 */
fun Modifier.silentClick(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = this.composed {
    val onClickUpdate by rememberUpdatedState(onClick)

    this.semantics(mergeDescendants = true) {
        if (role != null) this.role = role
        if (!enabled) this.disabled()
    }.pointerInput(enabled) {
        if (enabled) detectTapGestures(onTap = { onClickUpdate() })
    }
}