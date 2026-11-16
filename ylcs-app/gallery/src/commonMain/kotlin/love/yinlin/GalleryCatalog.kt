package love.yinlin

import love.yinlin.foundation.fts.FullTextSearch
import love.yinlin.page.*

internal enum class PageGroup(val title: String, val description: String) {
    Foundation("设计基础", "界面配色、排版和文字样式。"),
    Interaction("操作与反馈", "输入控件、选择器、导航和操作反馈。"),
    Layout("内容与布局", "布局容器、图片和数据展示。"),
    Pagination("Pagination", "分页数据管理、下拉刷新与加载布局。"),
}

internal object GalleryCatalog {
    val entries: List<Page> = [
        ThemePage,
        TextPage,
        InputPage,
        PickerPage,
        NavigationPage,
        AnimationPage,
        FloatingPage,
        DragDropPage,
        PicturePage,
        ImagePage,
        ContainerPage,
        LayoutPage,
        CollectionPage,
        BlurPage,
        ReorderingPage,
        PlatformViewPage,
        WidgetPage,
        PaginationPage,
    ]

    private val allEntries: List<Page> = [OverviewPage] + entries
    val pageById: Map<String, Page> = allEntries.associateBy { it.id }
    val componentCount: Int = entries.sumOf { it.components.size }

    val searchIndex: FullTextSearch<Page> by lazy {
        FullTextSearch(allEntries) { page ->
            buildList {
                add(page.id)
                add(page.title)
                add(page.english)
                add(page.description)
                add(page.keywords)
                for ((val title, val description, val keywords, val previewAlias) in page.components) {
                    add(title)
                    add(description)
                    add(keywords)
                    previewAlias?.let { add(it) }
                }
            }
        }
    }
}
