package love.yinlin.page

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import love.yinlin.GalleryActions
import love.yinlin.GalleryCaption
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.layout.ReorderingColumn
import love.yinlin.compose.ui.layout.ReorderingItem
import love.yinlin.compose.ui.layout.ReorderingItemScope
import love.yinlin.compose.ui.layout.ReorderingRow
import love.yinlin.compose.ui.layout.rememberReorderingState
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.moveItem

@Stable
object ReorderingPage : Page() {
    @Composable
    override fun Content() {
        var reverse by rememberFalse()
        var rtl by rememberFalse()
        var longPress by rememberFalse()
        var variable by rememberFalse()
        var columns by rememberValueState(2)

        ComponentColumn {
            GalleryActions {
                TextButton(if (reverse) "恢复顺序布局" else "反向布局") { reverse = !reverse }
                TextButton(if (rtl) "切换 LTR" else "切换 RTL") { rtl = !rtl }
                TextButton(if (longPress) "切换拖柄" else "切换整项长按") { longPress = !longPress }
                TextButton(if (variable) "切换统一尺寸" else "切换不同尺寸") { variable = !variable }
                TextButton("网格：$columns 列／行") { columns = if (columns == 2) 3 else 2 }
            }
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                key(reverse, rtl, variable, columns) {
                    Component("LazyColumn / LazyRow") {
                        LazyDemo(kind = 0, vertical = true, reverse = reverse, longPress = longPress, variable = variable, columns = columns)
                        LazyDemo(kind = 0, vertical = false, reverse = reverse, longPress = longPress, variable = variable, columns = columns)
                    }
                    Component("LazyVerticalGrid / LazyHorizontalGrid") {
                        LazyDemo(kind = 1, vertical = true, reverse = reverse, longPress = longPress, variable = variable, columns = columns)
                        LazyDemo(kind = 1, vertical = false, reverse = reverse, longPress = longPress, variable = variable, columns = columns)
                    }
                    Component("LazyVerticalStaggeredGrid / LazyHorizontalStaggeredGrid") {
                        LazyDemo(kind = 2, vertical = true, reverse = reverse, longPress = longPress, variable = variable, columns = columns)
                        LazyDemo(kind = 2, vertical = false, reverse = reverse, longPress = longPress, variable = variable, columns = columns)
                    }
                    Component("ReorderingColumn") {
                        PlainDemo(vertical = true, longPress = longPress, variable = variable)
                    }
                    Component("ReorderingRow") {
                        PlainDemo(vertical = false, longPress = longPress, variable = variable)
                    }
                }
            }
        }
    }

    @Composable
    private fun PlainDemo(vertical: Boolean, longPress: Boolean, variable: Boolean) {
        var order by rememberState { List(4) { it } }
        var last by rememberState { "拖动拖柄；松手并回落后才修改数据" }
        var history by rememberState<List<String>> { [] }
        val settle: (Int, Int) -> Unit = { from, to ->
            order = order.toMutableList().also { it.moveItem(from, to) }
            last = "onSettle: $from → $to"
            history = ([last] + history).take(6)
        }
        val tile: @Composable ReorderingItemScope.(Int, Boolean) -> Unit = { item, _ ->
            Tile(item, vertical, variable = variable, longPress = longPress)
        }

        if (vertical) ReorderingColumn(order, key = { it }, onSettle = settle, modifier = Modifier.fillMaxWidth(), content = tile)
        else {
            ReorderingRow(
                order,
                key = { it },
                onSettle = settle,
                modifier = Modifier.fillMaxWidth().height(Theme.size.cell6).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                content = tile,
            )
        }

        GalleryCaption(last)
        if (history.isNotEmpty()) GalleryCaption("最近回调：${history.joinToString(" · ")}")
        GalleryCaption("已提交顺序：${order.joinToString { (it + 1).toString() }}")
    }

    @Composable
    private fun LazyDemo(kind: Int, vertical: Boolean, reverse: Boolean, longPress: Boolean, variable: Boolean, columns: Int) {
        val order = remember { List(36) { it }.toMutableStateList() }
        var last by rememberState { "拖动到边缘并停留，继续移动到屏幕外的条目" }
        var clicked by rememberValueState(-1)
        var history by rememberState<List<String>> { [] }
        val list = rememberLazyListState()
        val grid = rememberLazyGridState()
        val staggered = rememberLazyStaggeredGridState()

        val move: (Int, Int) -> Unit = { from, to ->
            order.moveItem(from - 1, to - 1)
            last = "onMove: $from → $to（包含 header 的容器索引）"
            history = (["onMove: $from → $to"] + history).take(6)
        }
        val settle: (Int, Int) -> Unit = { from, to ->
            last = "onSettle: $from → $to"
            history = ([last] + history).take(6)
        }

        val state = when (kind) {
            0 -> rememberReorderingState(list, settle, move)
            1 -> rememberReorderingState(grid, settle, move)
            else -> rememberReorderingState(staggered, settle, move)
        }

        val spacing = Arrangement.spacedBy(Theme.padding.v9)
        val modifier = Modifier.fillMaxWidth().height(Theme.size.cell1 * if (vertical) 1.5f else 1f)
            .background(Theme.color.backgroundVariant, Theme.shape.v8)
        val padding = Theme.padding.value9
        val tile: @Composable ReorderingItemScope.(Int, Boolean) -> Unit = { item, _ ->
            Tile(item, vertical, variable || kind == 2, longPress) { clicked = item }
        }

        GalleryCaption(when (kind) {
            0 -> if (vertical) "LazyColumn" else "LazyRow"
            1 -> if (vertical) "LazyVerticalGrid" else "LazyHorizontalGrid"
            else -> if (vertical) "LazyVerticalStaggeredGrid" else "LazyHorizontalStaggeredGrid"
        })

        when {
            kind == 0 && vertical -> LazyColumn(modifier, list, contentPadding = padding, reverseLayout = reverse, verticalArrangement = spacing) {
                item(key = ItemKey("header")) { Header() }
                items(order, key = { it }) { item ->
                    ReorderingItem(state, item, enabled = item != 7) { tile(item, it) }
                }
            }
            kind == 0 -> LazyRow(modifier, list, contentPadding = padding, reverseLayout = reverse, horizontalArrangement = spacing) {
                item(key = ItemKey("header")) { Header() }
                items(order, key = { it }) { item ->
                    ReorderingItem(state, item, enabled = item != 7) { tile(item, it) }
                }
            }
            kind == 1 && vertical -> LazyVerticalGrid(GridCells.Fixed(columns), modifier, grid, contentPadding = padding,
                reverseLayout = reverse, verticalArrangement = spacing, horizontalArrangement = spacing) {
                item(key = ItemKey("header"), span = { GridItemSpan(maxLineSpan) }) { Header() }
                items(order, key = { it }) { item ->
                    ReorderingItem(state, item, enabled = item != 7) { tile(item, it) }
                }
            }
            kind == 1 -> LazyHorizontalGrid(GridCells.Fixed(columns), modifier, grid, contentPadding = padding,
                reverseLayout = reverse, verticalArrangement = spacing, horizontalArrangement = spacing) {
                item(key = ItemKey("header"), span = { GridItemSpan(maxLineSpan) }) { Header() }
                items(order, key = { it }) { item ->
                    ReorderingItem(state, item, enabled = item != 7) { tile(item, it) }
                }
            }
            vertical -> LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(columns), modifier, staggered, contentPadding = padding,
                reverseLayout = reverse, verticalItemSpacing = Theme.padding.v9, horizontalArrangement = spacing) {
                item(key = ItemKey("header"), span = StaggeredGridItemSpan.FullLine) { Header() }
                items(order, key = { it }) { item ->
                    ReorderingItem(state, item, enabled = item != 7) { tile(item, it) }
                }
            }
            else -> LazyHorizontalStaggeredGrid(StaggeredGridCells.Fixed(columns), modifier, staggered, contentPadding = padding,
                reverseLayout = reverse, horizontalItemSpacing = Theme.padding.h9, verticalArrangement = spacing) {
                item(key = ItemKey("header"), span = StaggeredGridItemSpan.FullLine) { Header() }
                items(order, key = { it }) { item ->
                    ReorderingItem(state, item, enabled = item != 7) { tile(item, it) }
                }
            }
        }

        GalleryActions {
            TextButton("重置顺序", icon = Icons.ResetSettings) {
                state.cancelDrag()
                order.clear()
                order.addAll(List(36) { it })
                last = "已重置"
                history = []
            }
            if (clicked != -1) GalleryCaption(text = "点击了条目 ${clicked + 1}")
        }

        GalleryCaption(last)
        if (history.isNotEmpty()) GalleryCaption(text = "最近回调：${history.joinToString(" · ")}")
        GalleryCaption(text = "顺序：${order.joinToString { (it + 1).toString() }}")
    }

    @Composable
    private fun Header() {
        Text(text = "Header\n不可拖动", modifier = Modifier.padding(Theme.padding.value))
    }

    @Composable
    private fun ReorderingItemScope.Tile(
        item: Int,
        vertical: Boolean,
        variable: Boolean,
        longPress: Boolean,
        onClick: () -> Unit = {},
    ) {
        val enabled = item != 7
        val size = Theme.size.cell9 * if (variable) 1f + item % 4 * 0.75f else 1f
        Row(
            modifier = (if (vertical) Modifier.fillMaxWidth().heightIn(min = size) else Modifier.width(Theme.size.cell4 + size).fillMaxHeight())
                .background(Theme.color.surface, Theme.shape.v8)
                .then(if (longPress) Modifier.dragHandle(enabled = enabled, longPress = true) else Modifier)
                .clickable(onClick = onClick).padding(Theme.padding.value),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (enabled) "条目 ${item + 1}" else "条目 8 · 禁用", modifier = Modifier.weight(1f))
            if (!longPress) Icon(Icons.DragHandle, modifier = Modifier.dragHandle(enabled = enabled))
        }
    }
}
