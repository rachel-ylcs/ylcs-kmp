package love.yinlin.compose.ui.window

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.ui.container.SurfaceContainer
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import kotlin.math.roundToInt

@Stable
data class ContextMenuItem(
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
)

@Stable
class ContextMenuScope {
    private val menus: MutableList<ContextMenuItem> = []

    fun item(text: String, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
        menus += ContextMenuItem(text, enabled, onClick, icon)
    }

    internal fun <T> build(block: (ContextMenuItem) -> T): List<T> = menus.map(block)
}

private val LocalContextMenuItems = compositionLocalOf<List<ContextMenuItem>> { [] }

private class ContextMenuPositionProvider(private val position: Offset) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset = IntOffset(
        (anchorBounds.left + position.x.roundToInt()).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        (anchorBounds.top + position.y.roundToInt()).coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
    )
}

/**
 * 创建右键上下文菜单
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun ContextMenu(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    var position: Offset? by rememberState { null }
    val inheritedItems = LocalContextMenuItems.current

    LaunchedEffect(enabled) {
        if (!enabled) position = null
    }

    Box(
        modifier = Modifier.onPointerEvent(PointerEventType.Press) { event ->
            if (enabled && event.buttons.isSecondaryPressed && event.changes.none { it.isConsumed }) {
                position = event.changes.firstOrNull()?.position
                for (change in event.changes) change.consume()
            }
        },
        propagateMinConstraints = true,
    ) {
        content()

        val anchor = position
        if (enabled && anchor != null) {
            val items = ContextMenuScope().apply(menus).build { it } + inheritedItems
            if (items.isNotEmpty()) {
                Popup(
                    popupPositionProvider = remember(anchor) { ContextMenuPositionProvider(anchor) },
                    onDismissRequest = { position = null },
                    properties = PopupProperties(focusable = true),
                ) {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .heightIn(max = Theme.size.cell1 * 2)
                            .clip(Theme.shape.v7)
                            .background(Theme.color.surface)
                            .border(Theme.border.v7, Theme.color.outline, Theme.shape.v7)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SurfaceContainer {
                            for (item in items) {
                                ThemeContainer(if (item.enabled) LocalColor.current else Theme.color.disabledContent) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().clickable(enabled = item.enabled, role = Role.Button) {
                                            position = null
                                            item.onClick()
                                        }.padding(Theme.padding.value),
                                    ) {
                                        TextIconAdapter { idIcon, idText ->
                                            item.icon?.let { Icon(icon = it, modifier = Modifier.idIcon()) }
                                            SimpleEllipsisText(text = item.label, modifier = Modifier.idText())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 为普通上下文菜单和新文本上下文菜单注入额外条目
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ContextMenuProvider(
    menus: ContextMenuScope.() -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val owner = remember { Any() }
    val items = ContextMenuScope().apply { if (enabled) menus() }.build { it }
    val inheritedItems = LocalContextMenuItems.current

    CompositionLocalProvider(LocalContextMenuItems provides items + inheritedItems) {
        Box(
            modifier = Modifier.appendTextContextMenuComponents {
                items.forEachIndexed { index, menu ->
                    item(
                        key = owner to index,
                        label = menu.label,
                        enabled = menu.enabled,
                        leadingIcon = null, // 上下文菜单附加拿不到当前父组件的尺寸信息，盲目加图标会导致尺寸不一的问题
                    ) {
                        close()
                        menu.onClick()
                    }
                }
            },
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}
