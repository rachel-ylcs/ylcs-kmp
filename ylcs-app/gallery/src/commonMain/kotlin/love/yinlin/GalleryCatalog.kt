package love.yinlin

import love.yinlin.foundation.fts.FullTextSearch
import love.yinlin.page.*

internal enum class PageGroup(val title: String, val description: String) {
    Foundation("基础组件", "文本排版、图片展示和图像处理。"),
    Interaction("操作反馈", "输入控件、选择器和导航。"),
    Layout("内容布局", "布局、容器、集合与浮窗。"),
    Effects("视觉效果", "动画、模糊和可实时预览的着色器。"),
    AdvancedInteraction("高级交互", "拖放操作和拖动排序。"),
    Standalone("独立组件", "原生视图、分页和专用组件。"),
}

internal object GalleryCatalog {
    val entries: List<Page> = [
        ThemePage,
        TextPage,
        PicturePage,
        ImagePage,
        InputPage,
        PickerPage,
        NavigationPage,
        LayoutPage,
        ContainerPage,
        CollectionPage,
        FloatingPage,
        AnimationPage,
        BlurPage,
        ShaderPage,
        DragDropPage,
        ReorderingPage,
        PlatformViewPage,
        PaginationPage,
        WidgetPage,
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
