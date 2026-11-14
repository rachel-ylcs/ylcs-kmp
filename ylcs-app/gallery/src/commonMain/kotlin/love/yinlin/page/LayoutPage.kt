package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.GalleryActions
import love.yinlin.GalleryCaption
import love.yinlin.GalleryParagraph
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.ui.image.ResourceFileImage
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.graphics.HSV
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.HorizontalComparator
import love.yinlin.compose.ui.layout.VerticalComparator
import love.yinlin.compose.ui.layout.HorizontalScrollContainer
import love.yinlin.compose.ui.layout.HorizontalSplitter
import love.yinlin.compose.ui.layout.OffsetBox
import love.yinlin.compose.ui.layout.VerticalSplitter
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderDpConverter
import love.yinlin.compose.ui.input.SliderFloatConverter
import love.yinlin.compose.ui.input.SliderIntConverter
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.Grid
import love.yinlin.compose.ui.text.Text
import love.yinlin.gallery.resources.Res

@Stable
object LayoutPage : Page(
    id = "layout",
    icon = Icons.GridOn,
    title = "布局",
    english = "Layout",
    description = "安排子项的位置、对齐和可滚动区域。",
    keywords = "网格 对齐 滚动",
    group = PageGroup.Layout,
) {
    private val demoGrid = component("Grid", "用行列组织内容，支持跨格、命名区域和响应式布局。", "GridLayoutBuilder MinMax Fr")
    private val demoOffsetBox = component("OffsetBox", "让内容在原位置的基础上水平或垂直偏移。", "偏移")
    private val demoActionScope = component("ActionScope", "把一组操作按钮对齐到容器末端。", "操作栏")
    private val demoHorizontalScroll = component("横向滚动", "用鼠标滚轮浏览横向列表或分页内容。", "HorizontalScrollContainer LazyRow HorizontalPager", previewAlias = "HorizontalScrollContainer")
    private val demoSplitView = component("分隔", "拖动分隔线调整面板大小，或对照两张图片。", "Splitter Comparator HorizontalSplitter VerticalSplitter HorizontalComparator VerticalComparator", previewAlias = "Splitter & Comparator")

    @Composable
    override fun Content() {
        ComponentColumn(alwaysShowTabs = true) {
            Component(demoGrid) {
                GalleryParagraph("Grid 适合搭建页面和卡片。长列表可以放在网格里独立滚动。")
                GalleryCaption("x → 列 / y → 行 / sx、sy、s → 间距 / t → 命名区域 / Modifier.item → 放置、跨度和对齐")
                GalleryParagraph("坐标从 1 开始，0 由网格自动安排，负数从末尾数起。")

                FixedGridExample()
                AutoPlacementExample()
                TrackSizingExample()
                IntrinsicSizingExample()
                MinMaxExample()
                PositionExample()
                SpanAlignmentExample()
                NamedAreasExample()
                OverlayExample()
                LazyListExample()
            }

            Component(demoOffsetBox) {
                var x by rememberValueState(0.5f)
                var y by rememberValueState(0.5f)
                ExampleRow(maxItemsInEachRow = 2) {
                    Example("x · ${(-x * 100).toInt()} dp", Modifier.weight(1f)) {
                        Slider(x, { x = it }, onValueChange = { x = it }, modifier = Modifier.fillMaxWidth())
                    }
                    Example("y · ${(-y * 100).toInt()} dp", Modifier.weight(1f)) {
                        Slider(y, { y = it }, onValueChange = { y = it }, modifier = Modifier.fillMaxWidth())
                    }
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.color.backgroundVariant)
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(Theme.size.cell4 + 220.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(Theme.size.cell4).background(Theme.color.primaryContainer)) {
                            OffsetBox(
                                x = (x * -100f).dp,
                                y = (y * -100f).dp
                            ) {
                                Box(modifier = Modifier.size(Theme.size.cell4).background(Theme.color.secondaryContainer))
                            }
                        }
                    }
                }
            }

            Component(demoActionScope) {
                ActionScope.Right.Container(modifier = Modifier.fillMaxWidth()) {
                    Icon(icon = Icons.Download, onClick = {})
                    Icon(icon = Icons.Upload, onClick = {})
                    Icon(icon = Icons.CloudDownload, onClick = {})
                    Icon(icon = Icons.CloudUpload, onClick = {})
                }
            }

            Component(demoHorizontalScroll) {
                val num = 30
                val colorList = remember(num) {
                    List(num) {
                        val normalizedIndex = (it - 1) % num
                        val hue = normalizedIndex * (360f / num)
                        val saturation = 0.7f
                        val value = 0.9f
                        HSV(hue, saturation, value).color
                    }
                }

                Text(
                    text = "Use the mouse wheel to swipe directly horizontally without pressing SHIFT.",
                    style = Theme.typography.v5.bold,
                    color = Theme.color.secondary
                )

                ExampleRow {
                    Example("Row + horizontalScroll", modifier = Modifier.fillMaxWidth()) {
                        val state = rememberScrollState()

                        HorizontalScrollContainer(state) {
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(state)) {
                                repeat(num) {
                                    Text(
                                        text = "Item $it",
                                        modifier = Modifier.background(colorList[it]).padding(Theme.padding.value)
                                    )
                                }
                            }
                        }
                    }
                    Example("LazyRow", modifier = Modifier.fillMaxWidth()) {
                        val state = rememberLazyListState()

                        HorizontalScrollContainer(state) {
                            LazyRow(state = state, modifier = Modifier.fillMaxWidth()) {
                                items(num) {
                                    Text(
                                        text = "Item $it",
                                        modifier = Modifier.background(colorList[it]).padding(Theme.padding.value)
                                    )
                                }
                            }
                        }
                    }
                    Example("HorizontalPager", modifier = Modifier.fillMaxWidth()) {
                        val state = rememberPagerState { num }

                        HorizontalScrollContainer(state) {
                            HorizontalPager(
                                state = state,
                                modifier = Modifier
                                    .width(Theme.size.cell3)
                                    .aspectRatio(1f)
                                    .background(Theme.color.error.copy(alpha = 0.25f))
                                    .padding(Theme.padding.eValue7)
                            ) {
                                Text(
                                    text = "Item $it",
                                    modifier = Modifier.background(colorList[it]).padding(Theme.padding.value)
                                )
                            }
                        }
                    }
                }
            }

            Component(demoSplitView) {
                GalleryCaption("Splitter · 默认仅显示分隔线，悬停或拖拽时显示浮动抓手")
                GalleryParagraph("拖动分隔条可调整两侧区域的大小。")
                ExampleRow(maxItemsInEachRow = 2) {
                    Example("水平分隔", Modifier.weight(1f)) {
                        HorizontalSplitter(
                            modifier = Modifier.fillMaxWidth().height(Theme.size.cell4),
                            left = {
                                Box(Modifier.fillMaxSize().background(Theme.color.primaryContainer), contentAlignment = Alignment.Center) {
                                    Text("LEFT", color = Theme.color.onContainer)
                                }
                            },
                            right = {
                                Box(Modifier.fillMaxWidth().height(Theme.size.cell8).background(Theme.color.secondaryContainer), contentAlignment = Alignment.Center) {
                                    Text("RIGHT", color = Theme.color.onContainer)
                                }
                            },
                        )
                    }
                    Example("垂直分隔", Modifier.weight(1f)) {
                        VerticalSplitter(
                            modifier = Modifier.fillMaxWidth().height(Theme.size.cell4 + Theme.size.cell4),
                            top = {
                                Box(Modifier.width(Theme.size.cell6).fillMaxHeight().background(Theme.color.primaryContainer), contentAlignment = Alignment.Center) {
                                    Text("TOP", color = Theme.color.onContainer)
                                }
                            },
                            bottom = {
                                Box(Modifier.fillMaxSize().background(Theme.color.secondaryContainer), contentAlignment = Alignment.Center) {
                                    Text("BOTTOM", color = Theme.color.onContainer)
                                }
                            },
                        )
                    }
                }

                GalleryCaption("Comparator · 双向箭头抓手始终可见，激活时切换主题色")
                GalleryParagraph("拖动分隔线，对比两张图或同一张图的不同画质。")
                ExampleRow(maxItemsInEachRow = 2) {
                    Example("示例 1 · 低画质与原图", Modifier.weight(1f)) {
                        HorizontalComparator(
                            modifier = Modifier.fillMaxWidth().height(Theme.size.image1),
                            left = {
                                ResourceFileImage(
                                    Res.getUri("drawable/img0.webp"),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = "低画质图片",
                                    options = ImageOptions(quality = ImageQuality.Low),
                                )
                            },
                            right = {
                                ResourceFileImage(
                                    Res.getUri("drawable/img0.webp"),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = "原始画质图片",
                                    options = ImageOptions(quality = ImageQuality.Full),
                                )
                            },
                        )
                    }
                    Example("示例 2 · 两张图片拼接", Modifier.weight(1f)) {
                        VerticalComparator(
                            modifier = Modifier.fillMaxWidth().height(Theme.size.image1),
                            top = {
                                ResourceFileImage(
                                    Res.getUri("drawable/img1.webp"),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = "拼接图片上半部分",
                                )
                            },
                            bottom = {
                                ResourceFileImage(
                                    Res.getUri("drawable/img2.webp"),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = "拼接图片下半部分",
                                )
                            },
                        )
                    }
                }
            }

        }
    }

    @Composable
    private fun FixedGridExample() {
        var columnGap by rememberValueState(Theme.padding.h)
        var rowGap by rememberValueState(Theme.padding.v)
        var gapMode by rememberValueState(0)
        val modes = ["s(x, y)", "sx / sy"]
        val columnWidth = Theme.size.cell6
        val rowHeight = Theme.size.cell9

        Example("01 · 固定轨道与横纵间距", Modifier.fillMaxWidth()) {
            GalleryParagraph("子项按行填入网格。调整行距和列距，看看留白的变化。")
            Filter(size = modes.size, selectedProvider = { gapMode == it }, titleProvider = { modes[it] }, onClick = { index, selected -> if (selected) gapMode = index })
            GalleryCaption("列间距 ${columnGap.value.toInt()} dp")
            Slider(columnGap, SliderDpConverter(0.dp, Theme.padding.h4), { columnGap = it }, onValueChange = { columnGap = it }, modifier = Modifier.fillMaxWidth())
            GalleryCaption("行间距 ${rowGap.value.toInt()} dp")
            Slider(rowGap, SliderDpConverter(0.dp, Theme.padding.v4), { rowGap = it }, onValueChange = { rowGap = it }, modifier = Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Grid(builder = {
                    x(columnWidth, columnWidth)
                    y(rowHeight, rowHeight, rowHeight)
                    if (gapMode == 0) s(columnGap, rowGap)
                    else {
                        sx(columnGap)
                        sy(rowGap)
                    }
                }) {
                    repeat(6) { index ->
                        GridTile("${index + 1}", Modifier.fillMaxSize())
                    }
                }
            }
            GalleryCaption("x(${columnWidth.value.toInt()}.dp, ${columnWidth.value.toInt()}.dp)\ny(${rowHeight.value.toInt()}.dp, ${rowHeight.value.toInt()}.dp, ${rowHeight.value.toInt()}.dp)")
        }
    }

    @Composable
    private fun AutoPlacementExample() {
        var columns by rememberValueState(3)
        var count by rememberValueState(8)
        val gap = Theme.padding.h
        val height = Theme.size.cell10

        Example("02 · 自动放置与隐式轨道", Modifier.fillMaxWidth()) {
            GalleryParagraph("一行排满后，网格会自动添加新行。")
            Filter(size = 3, selectedProvider = { columns == it + 2 }, titleProvider = { "${it + 2} 列" }, onClick = { index, selected -> if (selected) columns = index + 2 })
            GalleryCaption("项目数 · $count")
            Slider(count, SliderIntConverter(1, 12), { count = it }, onValueChange = { count = it }, modifier = Modifier.fillMaxWidth())
            Grid(builder = {
                repeat(columns) { x(1) }
                s(gap)
            }, modifier = Modifier.fillMaxWidth()) {
                repeat(count) { index ->
                    GridTile("${index + 1}", Modifier.fillMaxWidth().height(height))
                }
            }
            GalleryCaption("repeat($columns) { x(1) }\n// 不配置 y；直接发出子项，不调用 item")
        }
    }

    @Composable
    private fun TrackSizingExample() {
        var percentage by rememberValueState(0.25f)
        var height by rememberValueState(Theme.size.cell1)
        val fixedHeight = Theme.size.cell10
        val gap = Theme.padding.v
        val ratioHeight = Theme.size.cell9

        Example("03 · Dp / Float / Int / Fr 混合尺寸", Modifier.fillMaxWidth()) {
            GalleryParagraph("固定尺寸、百分比、内容尺寸和弹性轨道可以混用。")
            GalleryCaption("百分比行 · ${(percentage * 100).toInt()}%")
            Slider(percentage, SliderFloatConverter(0.1f, 0.4f), { percentage = it }, onValueChange = { percentage = it }, modifier = Modifier.fillMaxWidth())
            GalleryCaption("容器高度 · ${height.value.toInt()} dp")
            Slider(height, SliderDpConverter(Theme.size.cell1, Theme.size.cell1 + Theme.size.cell3), { height = it }, onValueChange = { height = it }, modifier = Modifier.fillMaxWidth())
            Grid(builder = {
                x(1)
                y(fixedHeight, percentage, 1, auto)
                s(gap)
            }, modifier = Modifier.fillMaxWidth().height(height)) {
                GridTile("Dp · ${fixedHeight.value.toInt()} dp", Modifier.fillMaxSize())
                GridTile("Float · ${(percentage * 100).toInt()}%", Modifier.fillMaxSize(), Theme.color.secondaryContainer)
                GridTile("Int · 1fr，分配剩余高度", Modifier.fillMaxSize(), Theme.color.tertiaryContainer)
                GridTile("Auto · 内容高度", Modifier.fillMaxWidth())
            }
            Grid(builder = {
                x(1, 2, Fr(3f))
                y(ratioHeight)
                sx(gap)
            }, modifier = Modifier.fillMaxWidth()) {
                GridTile("1", Modifier.fillMaxSize())
                GridTile("2", Modifier.fillMaxSize(), Theme.color.secondaryContainer)
                GridTile("Fr(3f)", Modifier.fillMaxSize(), Theme.color.tertiaryContainer)
            }
            GalleryCaption("y(${fixedHeight.value.toInt()}.dp, ${percentage}f, 1, auto)\nx(1, 2, Fr(3f)) → 剩余宽度按 1 : 2 : 3 分配\n注意：1 是 1fr，1f 是 100%，两者含义不同。")
        }
    }

    @Composable
    private fun IntrinsicSizingExample() {
        var width by rememberValueState(Theme.size.cell1 + Theme.size.cell3)
        val minWidth = Theme.size.cell1 + Theme.size.cell6
        val maxWidth = Theme.size.cell1 + Theme.size.cell1 + Theme.size.cell10
        val gap = Theme.padding.h
        val text = "Compose Grid layout"

        Example("04 · min / max / auto 内容尺寸", Modifier.fillMaxWidth()) {
            GalleryParagraph("拖动网格宽度，比较内容尺寸、自动宽度和文字换行。")
            GalleryCaption("网格宽度 · ${width.value.toInt()} dp；窄窗口可横向滚动")
            Slider(width, SliderDpConverter(minWidth, maxWidth), { width = it }, onValueChange = { width = it }, modifier = Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Grid(builder = {
                    x(min, max, auto)
                    y(auto)
                    sx(gap)
                }, modifier = Modifier.width(width)) {
                    GridTile(text, Modifier.fillMaxWidth())
                    GridTile(text, Modifier.fillMaxWidth(), Theme.color.secondaryContainer)
                    GridTile(text, Modifier.fillMaxWidth(), Theme.color.tertiaryContainer)
                }
            }
            GalleryCaption("x(min, max, auto) / y(auto)\n左 → 右：MinContent / MaxContent / Auto")
        }
    }

    @Composable
    private fun MinMaxExample() {
        var height by rememberValueState(Theme.size.cell1)
        val minimum = Theme.size.cell6
        val fixed = Theme.size.cell4
        val gap = Theme.padding.v

        Example("05 · minmax 为弹性轨道设置下限", Modifier.fillMaxWidth()) {
            GalleryParagraph("弹性轨道先满足最小尺寸，再按权重分配剩余空间。")
            GalleryCaption("容器高度 · ${height.value.toInt()} dp；第一行至少 ${minimum.value.toInt()} dp")
            Slider(height, SliderDpConverter(Theme.size.cell1, Theme.size.cell1 + Theme.size.cell3), { height = it }, onValueChange = { height = it }, modifier = Modifier.fillMaxWidth())
            Grid(builder = {
                x(1)
                y(minmax(minimum, Fr(1f)), 1, fixed)
                sy(gap)
            }, modifier = Modifier.fillMaxWidth().height(height)) {
                GridTile("MinMax ≥ ${minimum.value.toInt()} dp", Modifier.fillMaxSize())
                GridTile("Flex · 1fr", Modifier.fillMaxSize(), Theme.color.secondaryContainer)
                GridTile("Fixed · ${fixed.value.toInt()} dp", Modifier.fillMaxSize(), Theme.color.tertiaryContainer)
            }
            GalleryCaption("y(minmax(${minimum.value.toInt()}.dp, Fr(1f)), 1, ${fixed.value.toInt()}.dp)")
        }
    }

    @Composable
    private fun PositionExample() {
        val height = Theme.size.cell9
        val gap = Theme.padding.h

        Example("06 · 混合放置、单轴指定与负索引", Modifier.fillMaxWidth()) {
            GalleryParagraph("指定行列可放置子项，其余子项会填入空位。")
            Grid(builder = {
                x(1, 1, 1)
                y(height, height, height)
                s(gap)
            }, modifier = Modifier.fillMaxWidth()) {
                GridTile("(2, 2)", Modifier.item(x = 2, y = 2).fillMaxSize(), Theme.color.secondaryContainer)
                GridTile("(-1, -1)", Modifier.item(x = -1, y = -1).fillMaxSize(), Theme.color.tertiaryContainer)
                GridTile("x = 1", Modifier.item(x = 1).fillMaxSize())
                GridTile("y = 1", Modifier.item(y = 1).fillMaxSize())
                repeat(3) { index -> GridTile("自动 ${index + 1}", Modifier.fillMaxSize()) }
            }
            GalleryCaption("item(x = 2, y = 2) / item(x = -1, y = -1)\nitem(x = 1) / item(y = 1) / 无 item")
        }
    }

    @Composable
    private fun SpanAlignmentExample() {
        var alignmentIndex by rememberValueState(1)
        var fillArea by rememberFalse()
        val alignments = [Alignment.TopStart, Alignment.Center, Alignment.BottomEnd]
        val titles = ["TopStart", "Center", "BottomEnd"]
        val height = Theme.size.cell8
        val tileSize = Theme.size.cell10
        val gap = Theme.padding.h

        Example("07 · 跨行跨列与区域内对齐", Modifier.fillMaxWidth()) {
            GalleryParagraph("一个子项可以跨多行或多列，也可以在所占区域内对齐。")
            Filter(size = titles.size, selectedProvider = { alignmentIndex == it }, titleProvider = { titles[it] }, onClick = { index, selected -> if (selected) alignmentIndex = index })
            GalleryActions {
                Switch(checked = fillArea, onCheckedChange = { fillArea = it })
                Text("填满 A 的网格区域")
            }
            Grid(builder = {
                x(1, 1, 1)
                y(height, height, height)
                s(gap)
            }, modifier = Modifier.fillMaxWidth().background(Theme.color.backgroundVariant, Theme.shape.v3)) {
                GridTile("A", Modifier.item(xs = 2, ys = 2, alignment = alignments[alignmentIndex]).then(if (fillArea) Modifier.fillMaxSize() else Modifier.size(tileSize)))
                GridTile("B", Modifier.fillMaxSize(), Theme.color.secondaryContainer)
                GridTile("C", Modifier.fillMaxSize(), Theme.color.secondaryContainer)
                GridTile("跨三列", Modifier.item(xs = 3).fillMaxSize(), Theme.color.tertiaryContainer)
            }
            GalleryCaption("item(xs = 2, ys = 2, alignment = Alignment.${titles[alignmentIndex]})\nitem(xs = 3) → 跨度内也包含轨道间距")
        }
    }

    @Composable
    private fun NamedAreasExample() {
        var forceCompact by rememberFalse()
        val coverWidth = Theme.size.cell4
        val breakpoint = Theme.size.cell1 + Theme.size.cell2
        val gap = Theme.padding.h

        Example("08 · 命名区域与响应式经典卡片", Modifier.fillMaxWidth()) {
            GalleryParagraph("宽窗口并排显示封面和文字，窄窗口改为上下排列。")
            GalleryActions {
                Switch(checked = forceCompact, onCheckedChange = { forceCompact = it })
                Text("强制单列预览")
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = forceCompact || maxWidth < breakpoint
                Grid(builder = {
                    if (compact) {
                        x(1)
                        y(coverWidth, auto, auto, auto, auto)
                        t("cover", x = 1, y = 1)
                        t("title", x = 1, y = 2)
                        t("subtitle", x = 1, y = 3)
                        t("description", x = 1, y = 4)
                        t("extra", x = 1, y = 5)
                    }
                    else {
                        x(coverWidth, 1)
                        y(auto, auto, auto, auto)
                        t("cover", x = 1, y = 1, ys = 4)
                        t("title", x = 2, y = 1)
                        t("subtitle", x = 2, y = 2)
                        t("description", x = 2, y = 3)
                        t("extra", x = 2, y = 4)
                    }
                    s(gap)
                }, modifier = Modifier.fillMaxWidth()) {
                    // 按语义区域放置，内容声明顺序无需跟随行列顺序。
                    Text("${if (compact) "单列" else "双列"} · 调整窗口宽度查看布局", Modifier.item("extra"), color = Theme.color.onSurfaceVariant)
                    GridTile("COVER", Modifier.item("cover").fillMaxSize())
                    Text("银临茶舍 · Grid 卡片", Modifier.item("title"), style = Theme.typography.v5.bold)
                    Text("封面 / 标题 / 副标题 / 介绍 / 附加信息", Modifier.item("subtitle"), color = Theme.color.secondary)
                    Text("命名区域让内容与物理坐标分离。相同的内容可以随可用宽度重新排列，并保留清晰的语义标识。", Modifier.item("description"))
                }
            }
            GalleryCaption("t(\"cover\", x = 1, y = 1, ys = 4)\nModifier.item(\"cover\") / Modifier.item(\"title\")\n断点使用卡片实际可用宽度 ${breakpoint.value.toInt()} dp")
        }
    }

    @Composable
    private fun OverlayExample() {
        var raiseBase by rememberFalse()
        val height = Theme.size.cell4
        val gap = Theme.padding.h

        Example("09 · 重叠命名区域与 zIndex", Modifier.fillMaxWidth()) {
            GalleryParagraph("区域可以重叠，用绘制顺序控制哪项在上层。")
            GalleryActions {
                Switch(checked = raiseBase, onCheckedChange = { raiseBase = it })
                Text("将 BASE 提到前面")
            }
            Grid(builder = {
                x(1, 1, 1)
                y(height)
                sx(gap)
                t("base", x = 1, y = 1, xs = 2)
                t("overlay", x = 2, y = 1, xs = 2)
            }, modifier = Modifier.fillMaxWidth()) {
                GridTile("BASE", Modifier.item("base").fillMaxSize().zIndex(if (raiseBase) 1f else 0f))
                GridTile("OVERLAY", Modifier.item("overlay").fillMaxSize(), Theme.color.secondaryContainer.copy(alpha = 0.85f))
            }
            GalleryCaption("t(\"base\", x = 1, y = 1, xs = 2)\nt(\"overlay\", x = 2, y = 1, xs = 2)\nModifier.item(\"base\").zIndex(${if (raiseBase) 1 else 0}f)")
        }
    }

    @Composable
    private fun LazyListExample() {
        val state = rememberLazyListState()
        val height = Theme.size.cell1
        val gap = Theme.padding.v

        Example("10 · Grid 中的 LazyColumn", Modifier.fillMaxWidth()) {
            GalleryParagraph("标题和页脚留在原位，网格中间的列表独立滚动。")
            Grid(builder = {
                x(minmax(0.dp, Fr(1f)))
                y(max, minmax(0.dp, Fr(1f)), max)
                sy(gap)
                t("header", x = 1, y = 1)
                t("list", x = 1, y = 2)
                t("footer", x = 1, y = 3)
            }, modifier = Modifier.fillMaxWidth().height(height)) {
                Text("固定标题 · 40 条示例数据", Modifier.item("header"), style = Theme.typography.v5.bold)
                LazyColumn(
                    state = state,
                    modifier = Modifier.item("list").fillMaxSize().clip(Theme.shape.v3),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    items(count = 40, key = { it }) { index ->
                        GridTile("条目 ${index + 1}", Modifier.fillMaxWidth().heightIn(min = Theme.size.cell10), Theme.color.secondaryContainer)
                    }
                }
                Text("固定页脚 · 当前首项 ${state.firstVisibleItemIndex + 1} / 40", Modifier.item("footer"), color = Theme.color.onSurfaceVariant)
            }
            GalleryCaption("x(minmax(0.dp, Fr(1f)))\ny(max, minmax(0.dp, Fr(1f)), max)\nGrid(modifier = Modifier.height(${height.value.toInt()}.dp))")
        }
    }

    @Composable
    private fun GridTile(text: String, modifier: Modifier = Modifier, color: Color = Theme.color.primaryContainer) {
        Box(
            modifier = modifier.clip(Theme.shape.v3).background(color).padding(Theme.padding.value),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Theme.color.onContainer, textAlign = TextAlign.Center)
        }
    }
}
