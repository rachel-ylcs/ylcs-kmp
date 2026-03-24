package love.yinlin.compose.ui.floating

import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.AnimationVisibility
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.fastAnimateRectBackground
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.tool.NavigationBack

@Stable
open class FAB {
    companion object {
        val Empty = FAB()
    }

    /**
     * FAB 的主按钮。为空时将不显示 FAB，其他参数将无效
     *
     * expandable == true 会在展开与不展开的状态切换
     *
     * expandable == false 触发 onClick
     */
    protected open val action: FABAction? = null

    /**
     * FAB 是否可以展开。
     *
     * 有 menu 并不代表一定就可以展开，
     * 例如存在 menu 但当页面处于顶部时 menu 才可以展开
     * 而只有列表滑动到下面时 icon 变成 回到顶部，此时 menu 不允许展开。
     */
    open val expandable: Boolean = false

    /**
     * FAB 的菜单
     */
    protected open val menus: List<FABAction> = emptyList()

    /**
     * FAB 的大小
     */
    protected open val size: Dp? = null

    @Composable
    private fun ActionButton(scope: CoroutineScope, action: FABAction, overrideClick: (() -> Boolean)? = null) {
        val buttonSize = size ?: Theme.size.input7
        val enabled by rememberDerivedState(action.enabledProvider)
        val backgroundColor = if (enabled) {
            action.backgroundColorProvider?.invoke() ?: Theme.color.primaryContainer
        } else Theme.color.disabledContainer
        val backgroundBrush = remember(backgroundColor) {
            Brush.radialGradient(
                0f to backgroundColor.compositeOver(Colors.White.copy(alpha = 0.2f)),
                0.4f to backgroundColor.copy(alpha = 0.9f),
                1f to backgroundColor.copy(alpha = 0.85f).compositeOver(Colors.Black.copy(alpha = 0.15f))
            )
        }

        val contentColor = if (enabled) {
            action.contentColorProvider?.invoke() ?: Theme.color.onContainer
        } else Theme.color.disabledContent
        val shape = Theme.shape.circle

        ThemeContainer(contentColor) {
            Box(
                modifier = Modifier.padding(horizontal = buttonSize / 4, vertical = buttonSize / 8)
                    .size(buttonSize)
                    .border(Theme.border.v9, LocalColor.current.copy(alpha = 0.5f), shape)
                    .clip(shape)
                    .background(backgroundBrush)
                    .clickable(enabled = enabled) {
                        if (overrideClick?.invoke() != true && action.onClick != null) {
                            scope.launch { action.onClick() }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon = action.iconProvider(), tip = action.tipProvider(), modifier = Modifier.fillMaxSize(fraction = 0.6f))
            }
        }
    }

    @Composable
    private fun ActionMenus(scope: CoroutineScope, mainAction: FABAction) {
        var expanded by rememberFalse()

        val duration = Theme.animation.duration.default

        NavigationBack(enabled = expanded) {
            expanded = false
        }

        DisposableEffect(Unit) {
            onDispose {
                expanded = false
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
                .fastAnimateRectBackground(Theme.color.scrim.copy(alpha = if (expanded) 0.6f else 0f))
                .condition(expanded) { silentClick { expanded = false } }
                .padding(Theme.padding.eValue),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                menus.fastForEachIndexed { index, action ->
                    val delay = (menus.size - 1 - index) * duration / menus.size
                    AnimationVisibility(
                        visible = expanded,
                        duration = duration,
                        enter = { fadeIn(tween(delayMillis = delay)) + slideInVertically(tween(delayMillis = delay, durationMillis = duration - delay)) { it } },
                        exit = { fadeOut(tween(duration)) + slideOutHorizontally(tween(durationMillis = duration)) }
                    ) {
                        ActionButton(scope, action) {
                            expanded = false
                            false
                        }
                    }
                }

                ActionButton(scope, mainAction) {
                    if (expandable) {
                        expanded = !expanded
                        true
                    } else false
                }
            }
        }
    }

    val visible: Boolean get() = action != null

    @Composable
    fun Land() {
        val scope = rememberCoroutineScope()

        action?.let { mainAction ->
            Box(modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .zIndex(Floating.Z_INDEX_FAB)
            ) {
                if (menus.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        ActionButton(scope, mainAction)
                    }
                }
                else ActionMenus(scope, mainAction)
            }
        }
    }
}