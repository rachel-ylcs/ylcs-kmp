package love.yinlin.compose.ui.window

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem as AwtItem
import androidx.compose.runtime.Composable

@Composable
actual fun ContextMenu(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    ContextMenuArea(
        items = {
            val scope = ContextMenuScope()
            scope.menus()
            scope.build { item ->
                AwtItem(item.label, item.enabled, item.onClick)
            }
        },
        enabled = enabled,
        content = content
    )
}

@Composable
actual fun ContextMenuProvider(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    ContextMenuDataProvider(
        items = {
            val scope = ContextMenuScope()
            if (enabled) scope.menus()
            scope.build { item ->
                AwtItem(item.label, item.enabled, item.onClick)
            }
        },
        content = content
    )
}