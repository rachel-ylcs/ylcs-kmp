package love.yinlin.compose.ui.window

import androidx.compose.runtime.Composable

@Composable
actual fun ContextMenu(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    // TODO:
}

@Composable
actual fun ContextMenuProvider(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    // TODO:
}
