package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.zIndex
import love.yinlin.compose.extension.rememberDerivedState

/**
 * 包裹一个 Lazy item，key 必须与 items 的 key 相同。普通项使用容器的 animateItem 动画，
 * 拖动项浮到最上层并跟随指针，松手后以弹簧动画回落。enabled 为 false 时不能作为移动目标，
 * 但插入操作仍可能改变它的索引；不包裹的 header/footer 同样不能作为目标。
 * 默认由框架绘制拖动态的状态层和阴影；可通过 [LocalReorderingItemStyle] 覆盖样式。
 */
@Composable
fun LazyItemScope.ReorderingItem(
    state: ReorderingState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ReorderingItemScope.(isDragging: Boolean) -> Unit,
) = ReorderingItemContent(state, key, modifier, Modifier.animateItem(), enabled, content)

/** 用于 LazyVerticalGrid / LazyHorizontalGrid，约定与 [ReorderingState] 相同。 */
@Composable
fun LazyGridItemScope.ReorderingItem(
    state: ReorderingState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ReorderingItemScope.(isDragging: Boolean) -> Unit,
) = ReorderingItemContent(state, key, modifier, Modifier.animateItem(), enabled, content)

/** 用于 LazyVerticalStaggeredGrid / LazyHorizontalStaggeredGrid。 */
@Composable
fun LazyStaggeredGridItemScope.ReorderingItem(
    state: ReorderingState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ReorderingItemScope.(isDragging: Boolean) -> Unit,
) = ReorderingItemContent(state, key, modifier, Modifier.animateItem(), enabled, content)

@Composable
internal fun ReorderingItemContent(
    state: ReorderingState,
    key: Any,
    modifier: Modifier,
    animation: Modifier,
    enabled: Boolean,
    content: @Composable ReorderingItemScope.(Boolean) -> Unit,
) {
    val itemScope = remember(state, key) { ReorderingItemScope(state, key) }
    val pinnable = LocalPinnableContainer.current
    val position = remember(state, key, pinnable) { ReorderingItemPosition(pinnable) }
    val dragging by rememberDerivedState(state, key) { state.isDragging(key) }
    val held by rememberDerivedState(state, key) { state.hasGesture(key) }
    val floating by rememberDerivedState(state, key) { state.isFloating(key) }
    val itemStyle = LocalReorderingItemStyle.current(dragging)

    DisposableEffect(state, key, enabled, position) {
        if (enabled) state.keys += key
        state.positions[key] = position
        onDispose {
            state.unregister(key, position)
        }
    }

    Box(
        modifier = modifier.then(if (held) Modifier else animation)
            .zIndex(if (floating) 1f else 0f)
            .onGloballyPositioned { position.update(it, state.layout.info, key) },
        propagateMinConstraints = true
    ) {
        Box(modifier = Modifier.graphicsLayer {
            val offset = state.offset(key)
            translationX = offset.x
            translationY = offset.y
        }.then(itemStyle).onGloballyPositioned { itemScope.coordinates = it }, propagateMinConstraints = true) {
            itemScope.content(dragging)
        }
    }
}
