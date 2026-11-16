package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.GalleryActions
import love.yinlin.GalleryBadge
import love.yinlin.GalleryCaption
import love.yinlin.GalleryParagraph
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.compose.ui.layout.PaginationGrid
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.layout.PagerPagination
import love.yinlin.compose.ui.layout.PagerPaginationCells
import love.yinlin.compose.ui.layout.rememberPagerPaginationState
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.text.Text

@Stable
object PaginationPage : Page(
    id = "pagination",
    icon = Icons.GridOn,
    title = "分页",
    english = "Pagination",
    description = "管理分页数据，并在列表、网格和瀑布流中完成下拉刷新与继续加载。",
    keywords = "分页 Pagination PaginationArgs PaginationColumn PaginationGrid PaginationStaggeredGrid 下拉刷新 加载更多",
    group = PageGroup.Pagination,
) {
    private val demoPagination = component(
        title = "Pagination",
        description = "使用分页状态管理数据、游标和去重，并通过不同布局完成下拉刷新与加载更多。",
        keywords = "Pagination PaginationArgs PaginationColumn PaginationGrid PaginationStaggeredGrid PullLayout",
        tabOrder = 0,
    )
    private val demoPagerPagination = component(
        title = "PagerPagination",
        description = "根据列数与最大行数计算每页容量，使用页码条切换数据页。",
        keywords = "PagerPagination 页码分页 Fixed Adaptive maxRow maxCount",
        tabOrder = 1,
    )

    @Composable
    override fun Content() {
        ComponentColumn(alwaysShowTabs = true) {
            Component(demoPagination) {
                PaginationDemo()
            }

            Component(demoPagerPagination) {
                PagerPaginationDemo()
            }
        }
    }
}

@Stable
private data class PaginationDemoItem(
    val id: Int,
    val score: Int,
    val title: String,
    val summary: String,
)

@Stable
private class PaginationDemoState : PaginationArgs<PaginationDemoItem, Int, Int, Int>(
    default = 0,
    default1 = 0,
    pageNum = 6,
) {
    override fun distinctValue(item: PaginationDemoItem): Int = item.id
    override fun offset(item: PaginationDemoItem): Int = item.score
    override fun arg1(item: PaginationDemoItem): Int = item.id
}

private val paginationDemoItems = List(27) { index ->
    val id = index + 1
    PaginationDemoItem(
        id = id,
        score = index / 3 + 1,
        title = ["Compose 布局实践", "跨平台状态管理", "界面组件设计", "异步数据加载", "交互体验优化"][index % 5],
        summary = when (index % 3) {
            0 -> "通过稳定的状态管理，让列表数据在刷新和追加时保持一致。"
            1 -> "为不同窗口尺寸提供合适的内容布局与滚动体验。"
            else -> "使用游标定位下一批数据，并避免重复条目。"
        },
    )
}

private fun fetchPaginationDemoItems(offset: Int, cursor: Int, pageNum: Int): List<PaginationDemoItem> =
    paginationDemoItems.filter { it.score > offset || (it.score == offset && it.id > cursor) }.take(pageNum)

@Composable
private fun PaginationDemo() {
    val page = remember { PaginationDemoState() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val staggeredGridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    var layout by remember { mutableIntStateOf(0) }

    val refresh: suspend () -> Unit = {
        delay(300)
        page.newData(fetchPaginationDemoItems(page.default, page.default1, page.pageNum))
    }
    val loadMore: suspend () -> Unit = {
        if (page.canLoading) {
            delay(300)
            page.moreData(fetchPaginationDemoItems(page.offset, page.arg1, page.pageNum))
        }
    }

    LaunchedEffect(page) {
        refresh()
    }

    GalleryParagraph("Pagination 负责维护数据、去重键、偏移量和加载状态；PaginationArgs 在此基础上额外保存下一次请求所需的参数。")
    GalleryParagraph("示例使用本地数据模拟请求。下拉刷新会重置数据，上拉加载会追加数据；也可以使用下方按钮触发操作。")

    GalleryActions {
        GalleryBadge("已加载 ${page.items.size} 项")
        GalleryBadge("pageNum ${page.pageNum}", Theme.color.secondary)
        GalleryBadge("offset ${page.offset} · arg1 ${page.arg1}", Theme.color.tertiary)
        GalleryBadge(if (page.canLoading) "可继续加载" else "已加载完毕", if (page.canLoading) Theme.color.primary else Theme.color.onSurfaceVariant)
    }

    GalleryActions {
        TextButton("刷新数据", icon = Icons.Refresh) {
            scope.launch { refresh() }
        }
        TextButton("加载更多", icon = Icons.KeyboardArrowDown) {
            scope.launch { loadMore() }
        }
        TextButton("清空列表", icon = Icons.Clear) {
            page.newData([])
        }
    }

    TabBar(
        size = 3,
        index = layout,
        onNavigate = { layout = it },
        titleProvider = { when (it) {
            0 -> "PaginationColumn"
            1 -> "PaginationGrid"
            else -> "PaginationStaggeredGrid"
        } },
        modifier = Modifier.fillMaxWidth(),
    )

    when (layout) {
        0 -> PaginationColumn(
            items = page.items,
            modifier = Modifier.fillMaxWidth().height(390.dp),
            key = { it.id },
            state = listState,
            canRefresh = true,
            canLoading = page.canLoading,
            onRefresh = refresh,
            onLoading = loadMore,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(4.dp),
            header = {
                GalleryCaption("下拉刷新 · 上拉加载更多")
            },
        ) { item ->
            PaginationListItem(item)
        }

        1 -> PaginationGrid(
            items = page.items,
            modifier = Modifier.fillMaxWidth().height(420.dp),
            key = { it.id },
            columns = GridCells.Adaptive(Theme.size.cell3),
            state = gridState,
            canRefresh = true,
            canLoading = page.canLoading,
            onRefresh = refresh,
            onLoading = loadMore,
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            header = {
                GalleryCaption("网格布局 · 下拉刷新 · 上拉加载更多")
            },
        ) { item ->
            PaginationGridItem(item)
        }

        else -> PaginationStaggeredGrid(
            items = page.items,
            modifier = Modifier.fillMaxWidth().height(420.dp),
            key = { it.id },
            columns = StaggeredGridCells.Adaptive(Theme.size.cell3),
            state = staggeredGridState,
            canRefresh = true,
            canLoading = page.canLoading,
            onRefresh = refresh,
            onLoading = loadMore,
            contentPadding = PaddingValues(4.dp),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            header = {
                GalleryCaption("瀑布流布局 · 下拉刷新 · 上拉加载更多")
            },
        ) { item ->
            PaginationGridItem(item)
        }
    }
}

@Composable
private fun PaginationListItem(item: PaginationDemoItem) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Theme.color.surface, Theme.shape.v8).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = item.id.toString().padStart(2, '0'),
            color = Theme.color.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            GalleryParagraph(item.summary)
        }
    }
}

@Composable
private fun PaginationGridItem(item: PaginationDemoItem) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Theme.color.surface, Theme.shape.v8).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height((68 + item.id % 3 * 20).dp)
                .background(Theme.color.primaryContainer, Theme.shape.v8),
            contentAlignment = Alignment.Center,
        ) {
            Text("ITEM ${item.id}", color = Theme.color.onContainer, fontWeight = FontWeight.SemiBold)
        }
        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        GalleryParagraph(item.summary)
    }
}


@Stable
private data class PagerPaginationDemoItem(
    val id: Int,
    val title: String,
    val summary: String,
)

private val pagerPaginationDemoItems = List(137) { index ->
    val id = index + 1
    PagerPaginationDemoItem(
        id = id,
        title = ["布局与测量", "跨平台组件", "状态管理", "交互设计", "数据分页"][index % 5],
        summary = when (index % 4) {
            0 -> "等宽单元格，内容高度由自身决定。"
            1 -> "当前页只组合有限数量的元素，不创建内部滚动容器。"
            2 -> "固定列数或根据最小单元宽度自动计算列数。"
            else -> "通过页码状态快速跳转到指定数据页。"
        },
    )
}

@Composable
private fun PagerPaginationDemo() {
    val state = rememberPagerPaginationState(initialPage = 7)
    var columnsMode by remember { mutableIntStateOf(0) }
    var maxRow by remember { mutableIntStateOf(2) }
    var enabled by remember { mutableStateOf(true) }

    GalleryParagraph("PagerPagination 根据列数和最大行数计算每页容量。单元格宽度始终相等，行高由该行内容决定；组件内部使用普通布局，不会与外层可滚动 Column 争夺滚动事件。")

    GalleryActions {
        GalleryBadge("第 ${state.currentPage} 页 / 共 ${state.pageCount} 页")
        GalleryBadge(if (enabled) "允许切换" else "已禁用", if (enabled) Theme.color.primary else Theme.color.secondary)
        TextButton("回到第一页", icon = Icons.Refresh) {
            state.goToPage(1)
        }
        TextButton(if (enabled) "禁用分页" else "启用分页") {
            enabled = !enabled
        }
    }

    TabBar(
        size = 2,
        index = columnsMode,
        onNavigate = { columnsMode = it },
        titleProvider = { if (it == 0) "Adaptive" else "Fixed(3)" },
        modifier = Modifier.fillMaxWidth(),
    )

    GalleryActions {
        TextButton("最多 1 行") { maxRow = 1 }
        TextButton("最多 2 行") { maxRow = 2 }
        TextButton("最多 3 行") { maxRow = 3 }
        GalleryBadge("maxRow = $maxRow", Theme.color.secondary)
    }

    PagerPagination(
        items = pagerPaginationDemoItems,
        columns = if (columnsMode == 0) PagerPaginationCells.Adaptive(Theme.size.cell3) else PagerPaginationCells.Fixed(3),
        maxRow = maxRow,
        state = state,
        maxCount = 8,
        enable = enabled,
        contentPadding = Theme.padding.value10,
        horizontalPadding = Theme.padding.h,
        verticalPadding = Theme.padding.v,
    ) { item ->
        Column(
            modifier = Modifier.fillMaxWidth().background(Theme.color.surface, Theme.shape.v8).padding(Theme.padding.value),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.g),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height((48 + item.id % 4 * 14).dp)
                    .background(Theme.color.primaryContainer, Theme.shape.v8),
                contentAlignment = Alignment.Center,
            ) {
                Text("ITEM ${item.id}", color = Theme.color.onContainer, fontWeight = FontWeight.SemiBold)
            }
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            GalleryParagraph(item.summary)
        }
    }
}
