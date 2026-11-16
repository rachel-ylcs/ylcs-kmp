package love.yinlin.compose.ui.layout

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key as composeKey
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.text.Text

/**
 * 基于页码的分页网格。
 *
 * 该组件使用 Grid 自动放置当前页的有限数据，不创建内部滚动容器；
 * 显式定义等宽列轨道，行轨道按内容隐式生成。
 *
 * @param items 全部数据，组件根据其长度计算总页数。
 * @param columns 固定列数或按最小单元宽度自适应列数。
 * @param maxRow 每页最多显示的行数。
 * @param state 分页状态，可读取当前页并通过 [PagerPaginationState.goToPage] 切换页面。
 * @param maxCount 页码条最多显示的页码项数量，包含省略号，最小为 5。
 * @param enable 是否允许切换页码；禁用时页码和左右箭头均不可操作。
 * @param contentPadding 数据网格内容区域的内边距，不作用于页码条。
 * @param horizontalPadding 同一行单元格之间的水平间距。
 * @param verticalPadding 不同行之间的垂直间距。
 * @param key 可选的稳定条目键。
 * @param itemContent 当前页单元格内容。
 */
@Composable
fun <T> PagerPagination(
    items: List<T>,
    columns: PagerPaginationCells,
    maxRow: Int,
    state: PagerPaginationState = rememberPagerPaginationState(),
    modifier: Modifier = Modifier,
    maxCount: Int = 8,
    enable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues.Zero,
    horizontalPadding: Dp = Theme.padding.h,
    verticalPadding: Dp = Theme.padding.v,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    require(maxRow > 0) { "maxRow must be greater than 0." }
    require(maxCount >= 5) { "maxCount must be at least 5." }
    require(horizontalPadding >= 0.dp) { "horizontalPadding cannot be negative." }
    require(verticalPadding >= 0.dp) { "verticalPadding cannot be negative." }

    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layoutDirection = LocalLayoutDirection.current
        val availableWidth = (maxWidth - contentPadding.calculateLeftPadding(layoutDirection) - contentPadding.calculateRightPadding(layoutDirection)).coerceAtLeast(0.dp)
        val columnCount = when (columns) {
            is PagerPaginationCells.Fixed -> columns.count
            is PagerPaginationCells.Adaptive -> {
                if (availableWidth == Dp.Infinity) 1
                else {
                    val availableWidthPx = with(density) { availableWidth.roundToPx() }
                    val spacingPx = with(density) { horizontalPadding.roundToPx() }.coerceAtLeast(0)
                    val minSizePx = with(density) { columns.minSize.roundToPx() }.coerceAtLeast(1)
                    ((availableWidthPx.toLong() + spacingPx) / (minSizePx.toLong() + spacingPx)).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
                }
            }
        }
        val pageSize = (columnCount.toLong() * maxRow).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val pageCount = if (items.isEmpty()) 0 else ((items.size.toLong() + pageSize - 1) / pageSize).toInt()
        val currentPage = if (pageCount == 0) state.currentPage else state.currentPage.coerceIn(1, pageCount)

        SideEffect {
            state.updatePageCount(pageCount)
        }

        val startIndex = ((currentPage - 1).toLong() * pageSize).coerceAtMost(items.size.toLong()).toInt()
        val endIndex = (startIndex.toLong() + pageSize).coerceAtMost(items.size.toLong()).toInt()
        val pageItems = items.subList(startIndex, endIndex)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
        ) {
            Grid(
                builder = {
                    repeat(columnCount) { x(minmax(0.dp)) }
                    s(horizontalPadding, verticalPadding)
                },
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
            ) {
                for ((index, item) in pageItems.withIndex()) {
                    composeKey(key?.invoke(item) ?: startIndex + index) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            propagateMinConstraints = true,
                        ) {
                            itemContent(item)
                        }
                    }
                }
            }

            if (pageCount > 1) {
                PagerPaginationBar(
                    currentPage = currentPage,
                    pageCount = pageCount,
                    maxCount = maxCount,
                    enable = enable,
                    onPageChange = state::goToPage,
                    onPrevious = state::previousPage,
                    onNext = state::nextPage,
                )
            }
        }
    }
}

@Composable
private fun PagerPaginationBar(
    currentPage: Int,
    pageCount: Int,
    maxCount: Int,
    enable: Boolean,
    onPageChange: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val pageTokens = calculatePagerPageTokens(currentPage, pageCount, maxCount)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerPaginationArrow(
            icon = Icons.KeyboardArrowLeft,
            enabled = enable && currentPage > 1,
            tip = "上一页",
            onClick = onPrevious,
        )

        Spacer(Modifier.size(Theme.padding.g3))

        for ((index, page) in pageTokens.withIndex()) {
            if (page == null) {
                Box(
                    modifier = Modifier.size(Theme.size.input9),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("…", color = Theme.color.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            else {
                PagerPaginationPageButton(
                    page = page,
                    selected = page == currentPage,
                    enabled = enable && page != currentPage,
                    onClick = { onPageChange(page) },
                )
            }

            if (index != pageTokens.lastIndex) Spacer(Modifier.size(Theme.padding.g3))
        }

        Spacer(Modifier.size(Theme.padding.g3))

        PagerPaginationArrow(
            icon = Icons.KeyboardArrowRight,
            enabled = enable && currentPage < pageCount,
            tip = "下一页",
            onClick = onNext,
        )
    }
}

@Composable
private fun PagerPaginationPageButton(
    page: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered = interactionSource.collectIsHoveredAsState().value
    val shape = Theme.shape.v8
    val background = when {
        selected -> Theme.color.primary
        !enabled -> Theme.color.disabledContainer
        hovered -> Theme.color.backgroundVariant
        else -> Theme.color.surface
    }
    val foreground = when {
        selected -> Theme.color.onContainer
        !enabled -> Theme.color.disabledContent
        else -> Theme.color.onSurface
    }
    val border = when {
        selected || hovered -> Theme.color.primary
        !enabled -> Theme.color.disabledContainer
        else -> Theme.color.outline
    }

    Box(
        modifier = Modifier.size(Theme.size.input9)
            .clip(shape)
            .background(background)
            .border(Theme.border.v7, border, shape)
            .hoverable(interactionSource, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .pointerIcon(PointerIcon.Hand, enabled = enabled),
        contentAlignment = Alignment.Center,
    ) {
        Text(page.toString(), color = foreground, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun PagerPaginationArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    tip: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered = interactionSource.collectIsHoveredAsState().value
    val shape = Theme.shape.v8
    val background = when {
        !enabled -> Theme.color.disabledContainer
        hovered -> Theme.color.backgroundVariant
        else -> Theme.color.surface
    }
    val foreground = if (enabled) Theme.color.onSurface else Theme.color.disabledContent
    val border = if (hovered && enabled) Theme.color.primary else Theme.color.outline

    Box(
        modifier = Modifier.size(Theme.size.input9)
            .clip(shape)
            .background(background)
            .border(Theme.border.v7, border, shape)
            .hoverable(interactionSource, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .pointerIcon(PointerIcon.Hand, enabled = enabled),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, modifier = Modifier.size(Theme.size.smallIcon), color = foreground, tip = tip)
    }
}

/**
 * 计算页码条显示项。null 表示省略号；返回数量不超过 [maxCount]。
 * 在保留首尾页的前提下，优先以当前页为中心向两侧扩展。
 */
internal fun calculatePagerPageTokens(currentPage: Int, pageCount: Int, maxCount: Int): List<Int?> {
    if (pageCount <= 0) return []
    require(maxCount >= 5) { "maxCount must be at least 5." }
    if (pageCount <= maxCount) return (1..pageCount).map { it }

    val current = currentPage.coerceIn(1, pageCount)
    var start = current
    var end = current

    fun tokenCount(startPage: Int, endPage: Int): Int {
        val visibleStart = maxOf(startPage, 2)
        val visibleEnd = minOf(endPage, pageCount - 1)
        val visibleCount = (visibleEnd - visibleStart + 1).coerceAtLeast(0)
        return 2 + (if (startPage > 2) 1 else 0) + visibleCount + (if (endPage < pageCount - 1) 1 else 0)
    }

    while (true) {
        val canExpandLeft = start > 2 && tokenCount(start - 1, end) <= maxCount
        val canExpandRight = end < pageCount - 1 && tokenCount(start, end + 1) <= maxCount
        if (!canExpandLeft && !canExpandRight) break

        val leftCount = current - start
        val rightCount = end - current
        if (canExpandLeft && (!canExpandRight || leftCount <= rightCount)) start--
        else end++
    }

    return buildList {
        add(1)
        if (start > 2) add(null)
        for (page in maxOf(start, 2)..minOf(end, pageCount - 1)) add(page)
        if (end < pageCount - 1) add(null)
        add(pageCount)
    }
}
