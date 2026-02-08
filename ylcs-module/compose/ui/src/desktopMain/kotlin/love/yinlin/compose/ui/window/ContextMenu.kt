package love.yinlin.compose.ui.window

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
class ContextMenuScope {
    private val menus = mutableListOf<ContextMenuItem>()

    fun item(text: String, onClick: () -> Unit) {
        menus += ContextMenuItem(text, onClick)
    }

    internal fun build(): List<ContextMenuItem> = menus
}

@Composable
fun ContextMenu(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    ContextMenuArea(
        items = {
            val scope = ContextMenuScope()
            scope.menus()
            scope.build()
        },
        enabled = enabled,
        content = content
    )
}

@Composable
fun ContextMenuProvider(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    ContextMenuDataProvider(
        items = {
            val scope = ContextMenuScope()
            if (enabled) scope.menus()
            scope.build()
        },
        content = content
    )
}