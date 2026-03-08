package love.yinlin.compose.ui.node

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.platform.inspector

private class SilentClickNode(
    var enabled: Boolean,
    var role: Role?,
    var onClick: () -> Unit
) : DelegatingNode(), SemanticsModifierNode, PointerInputModifierNode {
    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        if (enabled) detectTapGestures { onClick() }
    })

    fun update(enabled: Boolean, role: Role?, onClick: () -> Unit) {
        val oldEnabled = this.enabled
        this.enabled = enabled
        this.role = role
        this.onClick = onClick

        if (oldEnabled != enabled) pointerInputNode.resetPointerInputHandler()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this@SilentClickNode.role?.let { this.role = it }
        if (!enabled) this.disabled()
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) =
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    override fun onCancelPointerInput() = pointerInputNode.onCancelPointerInput()
}

private data class SilentClickElement(
    val enabled: Boolean,
    val role: Role?,
    val onClick: () -> Unit
) : ModifierNodeElement<SilentClickNode>() {
    override fun create(): SilentClickNode = SilentClickNode(enabled, role, onClick)
    override fun update(node: SilentClickNode) = node.update(enabled, role, onClick)
    override fun InspectorInfo.inspectableProperties() = inspector("SilentClick") {
        "enabled" bind enabled
        "role" bind (role ?: "null")
        "onClick" bind onClick
    }
}

/**
 * 静默点击
 *
 * 去除点击的水波纹, 但不影响其他事件消费
 *
 * 注意不能通过将clickable的indicator设置null实现，
 * 这样会拦截长按事件导致例如内部文本无法被鼠标选择等问题
 */
@Stable
fun Modifier.silentClick(enabled: Boolean = true, role: Role? = null, onClick: () -> Unit): Modifier =
    this then SilentClickElement(enabled, role, onClick)