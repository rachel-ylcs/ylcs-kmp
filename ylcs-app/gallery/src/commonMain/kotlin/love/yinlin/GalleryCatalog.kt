package love.yinlin

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.foundation.fts.FullTextSearch
import love.yinlin.page.*

@Stable
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
            PageItem("picture", Icons.GridOn, "图片集", "Icons & Picture", "比较图标大小、提示、操作反馈与九宫格配图排列。", PicturePage, PageGroup.Layout, [
                ComponentItem("Icon", "比较图标大小、悬浮提示、点击、异步加载与带底色的图标。", "LoadingIcon ColorIcon"),
                ComponentItem("NineGrid", "对照 1–9 张图片、视频标记和超过九张时的展示。", "Image Picture 配图 九宫格 视频"),
            ]),
            PageItem("image", Icons.Image, "图像", "Image", "从多种输入到动图、缩放、缓存与裁剪，交互预览独立实现的异步图片组件。", ImagePage, PageGroup.Layout, [
                ComponentItem("ResourceFileImage", "离线比较 JPEG、PNG、BMP、静态 WebP、交错 GIF 与 EXIF 方向样本。", "ImageSource Resource JPEG PNG BMP GIF WebP EXIF 格式 资源"),
                ComponentItem("WebImage", "点击公开小图预设或输入网络地址，观察下载进度、请求头、来源、重试与版本刷新。", "BasicImage Network headers ImageState ImageProgress status error origin HTTP PNG JPEG WebP 缓存"),
                ComponentItem("LocalFileImage / Bytes", "读取本地路径、准备内置本地文件，并比较字节输入的独立状态。", "ImageSource LocalFile Bytes ByteArrayImageInput File 本地文件"),
                ComponentItem("占位与重试", "使用内置输入切换空内容、网络加载、读取失败、损坏数据和恢复，对照默认状态图标、进度与小图边界。", "ImagePlaceholder rememberImageState loading empty error retry canRetry Icons.Image Icons.Refresh Icons.Error 警告 错误 进度 透明背景 小图 边界"),
                ComponentItem("布局与点击", "切换七种 ContentScale 与对齐方式，检查实际图片的点击范围及默认、单边尺寸布局。", "ContentScale alignment onClick placeholder aspectRatio size ImageState contentRect contentDescription 无障碍"),
                ComponentItem("外观与画质", "调节统一画质、圆形裁剪、主题着色、透明度和首次淡入，查看初次解码尺寸与布局缩放。", "ImageOptions ImageQuality Low Medium High Full circle alpha colorFilter fadeIn imageSize originalSize"),
                ComponentItem("动图与生命周期", "比较 GIF / WebP 播放与首帧模式，暂停、重播、卸载并切换 80 帧样本，检查 Android 系统 WebP 播放。", "ImageOptions animated isPlaying isAnimated GIF WebP Android AnimatedImageDrawable 透明 叠帧 disposal 循环 生命周期"),
                ComponentItem("图片缩放 / Pager", "使用双击、双指、滚轮、拖动和按钮缩放静态图与动图，检查翻页手势协作。", "ImageZoomState zoom scale translation reset HorizontalPager 手势"),
                ComponentItem("版本与缓存", "比较无版本与附加版本键，观察三个消费者的请求合并、状态重挂载、内存命中与全局缓存清理。", "ResourceFileImage version String? null ImageManager clearCache memoryCacheSize Memory Disk Network LRU 缓存 并发"),
                ComponentItem("cropImageRegion", "为资源和网络图片附加裁剪选区，拖动自由或固定比例区域并查看原图比例坐标。", "cropImageRegion ResourceFileImage WebImage ImageState CropImageState CropRegion aspectRatio 裁剪 比例"),
            ]),
            PageItem("blur", Icons.Cloud, "模糊", "Source & Target Blur", "连接背景来源与模糊面板，实时比较半径、颜色、噪点和采样质量。", BlurPage, PageGroup.Layout, [
                ComponentItem("Source / Target", "配置 BlurParameters 并通过 copy 调节参数，切换播放页与暂停面板参数，比较透明度、零半径和无噪点。", "BlurState BlurParameters blurSource blurTarget blurRadius backgroundColor tintColor noiseFactor qualityFraction"),
                ComponentItem("多个 Target", "同一背景连接不同半径和质量的圆角面板，移动面板并缩放窗口检查对齐与边缘。"),
                ComponentItem("动态与重挂载", "开关动态背景、来源和目标，替换状态并切换页面，检查画面保留与恢复。", "生命周期 KeepLastFrame Android iOS Desktop Web JS Wasm"),
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
            PageItem("reordering", Icons.DragHandle, "拖动排序", "Reordering", "拖动列表、网格与瀑布流，观察框架默认的激活状态层与抬升阴影、条目让位、边缘滚动和索引回调。", ReorderingPage, PageGroup.Layout, [
                ComponentItem("LazyColumn / LazyRow", "纵向与横向列表；可切换尺寸、拖柄、整项长按、反向布局和 RTL，查看最近的索引回调。", "rememberReorderingState ReorderingItem dragHandle onMove onSettle"),
                ComponentItem("LazyVerticalGrid / LazyHorizontalGrid", "两列／三列及横向网格的跨行跨列插入排序，含不同尺寸、header 和禁用目标。", "LazyGridState GridCells"),
                ComponentItem("LazyVerticalStaggeredGrid / LazyHorizontalStaggeredGrid", "不同尺寸的条目在瀑布流中排序，拖动到边缘可继续滚动。", "LazyStaggeredGridState StaggeredGridCells"),
                ComponentItem("ReorderingColumn", "少量条目在容器边界内拖动，回落中可继续拖动；记录每次提交的索引和最终顺序。", "歌词引擎 优先级 边界 onSettle"),
                ComponentItem("ReorderingRow", "在内容边界内拖动，可切换不同宽度及 RTL，回落中可连续拖动；在外部接入横向滚动。", "Row horizontalScroll 边界 onSettle"),
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
            PageItem("widget", Icons.Token, "组件", "Widgets", "更多丰富组件", WidgetPage, PageGroup.Layout, [
                ComponentItem("Calendar", "切换月份，查看日期、农历、节日与节气。", "日历 月历 日期 星期"),
                ComponentItem("LogOutput", "切换默认、固定与填满尺寸，在组件外过滤日志，暂停滚动后查看新增/总行数并回到底部。", "LogOutputState append ANSI filter 日志 行号 复制"),
                ComponentItem("HexView", "切换尺寸和数据样例，比较自适应列数与 ASCII 显示，点击或拖选字节并查看连续选区。", "HexViewState rememberHexViewState selectedRange select HEX ASCII ByteArray 十六进制 偏移 字节 拖选"),
                ComponentItem("DataTable", "数据管理表格，支持多种类型读写编辑、排序", "DataTableState DataTableItem DataTableCell DataTableColumn DataTableSortableCell"),
            ]),
        ]

        val allEntries: List<PageItem> = [overview] + entries
        val componentCount: Int = entries.sumOf { it.components.size }

        val searchIndex: FullTextSearch<PageItem> by lazy {
            FullTextSearch(allEntries) { item ->
                buildList {
                    add(item.title)
                    add(item.english)
                    add(item.description)
                    for (component in item.components) {
                        add(component.title)
                        add(component.description)
                        add(component.keywords)
                    }
                }
            }
        }

        fun forPage(page: Page): PageItem? = allEntries.find { it.page === page }
    }
}
