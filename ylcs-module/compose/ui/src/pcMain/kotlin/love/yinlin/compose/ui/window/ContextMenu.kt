package love.yinlin.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
data class ContextMenuItem(
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Stable
class ContextMenuScope {
    private val menus: MutableList<ContextMenuItem> = []

    fun item(text: String, enabled: Boolean = true, onClick: () -> Unit) {
        menus += ContextMenuItem(text, enabled, onClick)
    }

    internal fun <T> build(block: (ContextMenuItem) -> T): List<T> = menus.map(block)
}

/**
 * 创建右键上下文菜单
 */
@Composable
expect fun ContextMenu(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
)

/**
 * 注入额外的上下文菜单
 */
@Composable
expect fun ContextMenuProvider(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
)