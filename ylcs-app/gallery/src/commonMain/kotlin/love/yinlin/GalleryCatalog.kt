package love.yinlin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.page.*

@Immutable
internal data class ComponentItem(val title: String, val description: String, val keywords: String = "")

internal enum class PageGroup(val title: String) {
    Foundation("设计基础"),
    Interaction("操作与反馈"),
    Layout("内容与布局"),
}

@Stable
internal data class PageItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val english: String,
    val description: String,
    val page: Page,
    val group: PageGroup? = null,
    val components: List<ComponentItem> = [],
) {
    fun matches(query: String): Boolean = query.isBlank() ||
        title.contains(query, ignoreCase = true) || english.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) || components.any {
            it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) ||
                it.keywords.contains(query, ignoreCase = true)
        }

    companion object {
        val overview = PageItem("overview", Icons.Home, "总览", "Overview", "从设计基础到交互反馈，探索 Rachel UI。", OverviewPage)

        val entries: List<PageItem> = [
            PageItem("theme", Icons.Theme, "主题", "Theme", "认识颜色的角色，比较浅色与深色下的容器、内容和状态。", ThemePage, PageGroup.Foundation, [
                ComponentItem("Accent", "三种主题强调色，用于区分主要操作、次级操作与补充信息。", "primary secondary tertiary ColorSystem ColorTheme"),
                ComponentItem("Container", "对照容器底色与两级内容色，查看实际的文字对比。"),
                ComponentItem("Background / Surface", "页面背景、背景变体与表面色构成界面的层次。", "onBackground onSurface backgroundVariant"),
                ComponentItem("Status", "错误、警告与半透明遮罩，传达不同的反馈状态。"),
                ComponentItem("Outline / Disabled", "轮廓线与禁用状态的颜色，补齐界面的视觉边界。"),
            ]),
            PageItem("text", Icons.TextFields, "文本", "Typography & Text", "输入、排版与富文本，让信息既易读，也易于操作。", TextPage, PageGroup.Foundation, [
                ComponentItem("Input", "试试提示、多行、长度限制、清除按钮、只读与密码输入。", "PasswordInput InputState InputDecoration LengthViewer MaxLength MultiLine"),
                ComponentItem("Text", "切换加粗、斜体与文字装饰，对照 v1–v10 的中英文排版。"),
                ComponentItem("StrokeText", "为文字添加描边，观察轮廓与主题色的组合。"),
                ComponentItem("RichText", "同一段内容包含样式、链接与内联图标，并展示它的编码结果。"),
            ]),
            PageItem("input", Icons.CheckBox, "输入", "Controls & Input", "点击、选择、拖动，体验控件在可用、禁用和加载时的表现。", InputPage, PageGroup.Interaction, [
                ComponentItem("Button", "对照三种按钮层级、图标、文字与禁用状态；加载按钮模拟两秒任务。", "PrimaryButton SecondaryButton TertiaryButton PrimaryLoadingButton PrimaryTextButton PrimaryLoadingTextButton IconButton TextButton LoadingButton"),
                ComponentItem("Switch", "两个开关共享同一状态，另一个保留禁用状态。"),
                ComponentItem("Radio / CheckBox", "体验单选、多选和三态选择，并比较禁用选项。", "RadioGroup TriStateCheckBox"),
                ComponentItem("Slider", "拖动数值滑块，对照无滑块、禁用和字符转换示例。"),
                ComponentItem("ComboBox", "从列表中选择项目，比较默认宽度、自适应宽度与禁用状态。"),
                ComponentItem("Filter", "体验单选与多选筛选，部分项目携带图标或处于禁用状态。"),
                ComponentItem("ColorPicker", "拖动颜色选择器，观察颜色、透明度与预览的变化。"),
            ]),
            PageItem("image", Icons.Image, "图片", "Icons & Images", "从单个图标到九宫格，比较大小、加载反馈和图片排列。", ImagePage, PageGroup.Layout, [
                ComponentItem("Icon", "比较图标大小、悬浮提示、点击、异步加载与带底色的图标。", "LoadingIcon ColorIcon"),
                ComponentItem("NineGrid", "对照 1–9 张图片、视频标记和超过九张时的展示。"),
            ]),
            PageItem("animation", Icons.Animation, "动画", "Motion & Progress", "让等待、进度与展开过程拥有清晰、连贯的视觉反馈。", AnimationPage, PageGroup.Interaction, [
                ComponentItem("Loading", "观察环形与波浪加载动画，比较不同的等待反馈。", "CircleLoading WaveLoading"),
                ComponentItem("Progress", "同一份循环进度驱动线性与环形指示器。", "LinearProgress CircleProgress"),
                ComponentItem("ExpandableContent", "点击按钮展开或收起内容，观察高度与可见性的过渡。"),
            ]),
            PageItem("container", Icons.Package, "容器", "Surfaces & Layout", "调整表面、状态与内容布局，用真实操作理解容器的能力。", ContainerPage, PageGroup.Layout, [
                ComponentItem("Surface", "调整内边距、圆角、阴影、色调和边框，实时查看表面的变化。"),
                ComponentItem("OffsetBox", "拖动 x / y 控制器，观察内容相对于原始位置的偏移。"),
                ComponentItem("StatefulBox", "切换加载、空内容、错误与正常状态，比较容器如何呈现。"),
                ComponentItem("ReplaceableBox", "添加内容后点击替换颜色，长按移除内容。"),
                ComponentItem("AdderBox", "体验添加、替换与删除，最多展示九个项目。"),
                ComponentItem("Banner", "四张图片每三秒自动切换，展示轮播中的裁剪与布局。"),
                ComponentItem("ActionScope", "将一组操作排列在容器右侧，观察对齐与间距。"),
                ComponentItem("AdaptiveTwoBox", "调整窗口宽度，观察两个不同大小的内容如何重新排列。"),
                ComponentItem("HorizontalScrollContainer", "在横向列表上使用鼠标滚轮；Row、LazyRow 和 Pager 分别展示滚动方式。", "HorizontalPager"),
            ]),
            PageItem("collection", Icons.GridOn, "集合", "Trees & Tags", "用层级结构与可删除标签整理更多内容。", CollectionPage, PageGroup.Layout, [
                ComponentItem("TreeView", "展开层级节点，查看图标、缩进与嵌套的子节点。"),
                ComponentItem("TagView", "五十个标签按可用宽度换行，点击删除以观察集合更新。"),
            ]),
            PageItem("navigation", Icons.Anchor, "导航", "Tabs & Breadcrumbs", "切换当前内容，或者沿着路径回到更上层的位置。", NavigationPage, PageGroup.Interaction, [
                ComponentItem("TabBar", "切换五个标签，其中一个带图标，另一个处于禁用状态。"),
                ComponentItem("Breadcrumb", "点击任一上层路径截断导航，重置后可以再次体验完整路径。"),
            ]),
            PageItem("floating", Icons.Cloud, "浮窗", "Overlays & Feedback", "打开弹窗、抽屉与浮层，体验提示、进度和悬浮操作。", FloatingPage, PageGroup.Interaction, [
                ComponentItem("Dialog", "体验信息、确认、输入、选择、加载与进度弹窗。", "DialogInfo DialogConfirm DialogInput DialogChoice DialogLoading DialogProgress"),
                ComponentItem("Sheet", "比较普通内容、填满区域、携带参数与独立滚动列表的抽屉。", "SheetContent LazyColumn"),
                ComponentItem("Flyout", "悬停查看提示，点击打开浮层或菜单，再关闭它们。", "BalloonTip Menus Menu"),
                ComponentItem("Tip", "查看信息、成功、警告与错误四种提示。"),
                ComponentItem("FAB", "控制右下角悬浮按钮的可见性与展开菜单。"),
            ]),
            PageItem("widget", Icons.Token, "组件", "Calendar & Widgets", "把基础控件组合成完整的日历展示。", WidgetPage, PageGroup.Layout, [
                ComponentItem("Calendar", "切换月份，查看日期、农历、节日与节气。"),
            ]),
        ]

        val allEntries: List<PageItem> = [overview] + entries
        val componentCount: Int = entries.sumOf { it.components.size }

        fun forPage(page: Page): PageItem? = allEntries.find { it.page === page }
    }
}
