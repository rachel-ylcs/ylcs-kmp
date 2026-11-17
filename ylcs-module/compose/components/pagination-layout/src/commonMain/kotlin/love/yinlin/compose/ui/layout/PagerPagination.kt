package love.yinlin.compose.ui.layout

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.text.SimpleEllipsisText
import androidx.compose.foundation.layout.Grid as ComposeGrid
import androidx.compose.runtime.key as composeKey

/**
 * 基于页码的分页网格。
 *
 * @param items 全部数据，组件根据其长度计算总页数。
 * @param columns 固定列数或按最小单元宽度自适应列数。
 * @param maxRow 每页最多显示的行数。
 * @param state 分页状态
 * @param maxCount 页码条最多显示的页码项数量，包含省略号，默认 10，最小为 5。
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
    modifier: Modifier = Modifier,
    state: PagerPaginationState = remember { PagerPaginationState() },
    maxCount: Int = 10,
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

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = when (columns) {
            is PagerPaginationCells.Fixed -> columns.count
            is PagerPaginationCells.Adaptive -> {
                if (!constraints.hasBoundedWidth) 1
                else {
                    val layoutDirection = LocalLayoutDirection.current
                    with(LocalDensity.current) {
                        val left = contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
                        val right = contentPadding.calculateRightPadding(layoutDirection).roundToPx()
                        val availableWidth = (constraints.maxWidth.toLong() - left - right).coerceAtLeast(0L)
                        val spacing = horizontalPadding.roundToPx()
                        val minSize = columns.minSize.roundToPx().coerceAtLeast(1)
                        ((availableWidth + spacing) / (minSize.toLong() + spacing)).coerceAtLeast(1L).toInt()
                    }
                }
            }
        }
        val itemCount = items.size
        val pageSize = (columnCount.toLong() * maxRow).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val pageCount = if (itemCount == 0) 0 else (itemCount - 1) / pageSize + 1
        val currentPage = state.currentPage.coerceAtMost(pageCount.coerceAtLeast(1))

        LaunchedEffect(state, pageCount) {
            state.updatePageCount(pageCount)
        }

        val startIndex = (currentPage - 1) * pageSize
        val endIndex = startIndex + pageSize.coerceAtMost(itemCount - startIndex)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
        ) {
            PagerPaginationGrid(
                items = items,
                startIndex = startIndex,
                endIndex = endIndex,
                columnCount = columnCount,
                horizontalPadding = horizontalPadding,
                verticalPadding = verticalPadding,
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                key = key,
                itemContent = itemContent,
            )

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
private fun <T> PagerPaginationGrid(
    items: List<T>,
    startIndex: Int,
    endIndex: Int,
    columnCount: Int,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    modifier: Modifier,
    key: ((T) -> Any)?,
    itemContent: @Composable (T) -> Unit,
) {
    if (startIndex == endIndex) {
        Spacer(modifier)
        return
    }

    ComposeGrid(
        config = {
            val track = minmax(0.dp, 1.fr)
            repeat(columnCount) { column(track) }
            gap(row = verticalPadding, column = horizontalPadding)
        },
        modifier = modifier,
    ) {
        val iterator = items.listIterator(startIndex)
        for (index in startIndex ..< endIndex) {
            val item = iterator.next()
            composeKey(key?.invoke(item) ?: index) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    propagateMinConstraints = true,
                ) {
                    itemContent(item)
                }
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
    val pageTokens = remember(currentPage, pageCount, maxCount) {
        calculatePagerPageTokens(currentPage, pageCount, maxCount)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.g3, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerPaginationArrow(
            icon = Icons.KeyboardArrowLeft,
            enabled = enable && currentPage > 1,
            tip = "上一页",
            onClick = onPrevious,
        )

        for (page in pageTokens) {
            if (page == null) {
                Box(
                    modifier = Modifier.size(Theme.size.input9),
                    contentAlignment = Alignment.Center,
                ) {
                    SimpleEllipsisText("…", color = Theme.color.onSurfaceVariant, style = Theme.typography.v7)
                }
            }
            else {
                composeKey(page) {
                    val selected = page == currentPage
                    PagerPaginationButton(
                        enabled = enable && !selected,
                        onClick = { onPageChange(page) },
                        selected = selected,
                    ) { foreground ->
                        val text = remember { page.toString() }
                        val style = if (selected) Theme.typography.v8.bold else Theme.typography.v8
                        SimpleEllipsisText(text, color = foreground, style = style)
                    }
                }
            }
        }

        PagerPaginationArrow(
            icon = Icons.KeyboardArrowRight,
            enabled = enable && currentPage < pageCount,
            tip = "下一页",
            onClick = onNext,
        )
    }
}

@Composable
private fun PagerPaginationArrow(
    icon: ImageVector,
    enabled: Boolean,
    tip: String,
    onClick: () -> Unit,
) {
    PagerPaginationButton(
        enabled = enabled,
        onClick = onClick,
        disabledBorder = Theme.color.outline,
    ) { foreground ->
        Icon(icon, modifier = Modifier.size(Theme.size.smallIcon), color = foreground, tip = tip)
    }
}

@Composable
private fun PagerPaginationButton(
    enabled: Boolean,
    onClick: () -> Unit,
    selected: Boolean = false,
    disabledBorder: Color = Theme.color.disabledContainer,
    content: @Composable (Color) -> Unit,
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
        selected -> Theme.color.primary
        !enabled -> disabledBorder
        hovered -> Theme.color.primary
        else -> Theme.color.outline
    }

    Box(
        modifier = Modifier.size(Theme.size.input9)
            .clip(shape)
            .background(background)
            .border(Theme.border.v7, border, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).pointerIcon(PointerIcon.Hand, enabled = enabled),
        contentAlignment = Alignment.Center,
    ) {
        content(foreground)
    }
}

/**
 * 计算页码条显示项。null 表示省略号；返回数量不超过 [maxCount]。
 * 在保留首尾页的前提下，优先以当前页为中心向两侧扩展。
 */
internal fun calculatePagerPageTokens(currentPage: Int, pageCount: Int, maxCount: Int): List<Int?> {
    if (pageCount <= 0) return []
    require(maxCount >= 5) { "maxCount must be at least 5." }
    if (pageCount <= maxCount) return List(pageCount) { it + 1 }

    val windowSize = maxCount - 4
    val windowStart = currentPage.coerceIn(1, pageCount) - windowSize / 2
    val visiblePages = when {
        windowStart <= 3 -> 2 .. (maxCount - 2)
        windowStart >= pageCount - windowSize - 1 -> (pageCount - maxCount + 3) ..< pageCount
        else -> windowStart ..< (windowStart + windowSize)
    }

    return buildList(maxCount) {
        add(1)
        if (visiblePages.first > 2) add(null)
        for (page in visiblePages) add(page)
        if (visiblePages.last < pageCount - 1) add(null)
        add(pageCount)
    }
}
