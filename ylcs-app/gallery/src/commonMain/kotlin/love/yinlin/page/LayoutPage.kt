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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.GalleryActions
import love.yinlin.GalleryCaption
import love.yinlin.GalleryParagraph
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.graphics.HSV
import love.yinlin.compose.ui.layout.ActionScope
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

@Stable
object LayoutPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn(alwaysShowTabs = true) {
            Component("Grid") {
                GalleryParagraph("Grid 用于页面或复杂组件的二维结构布局，所有子项都会参与组合与测量；大量数据请使用懒网格，或参考下方在 Grid 单元格中放置 LazyColumn 的示例。")
                GalleryCaption("x → 列 / y → 行 / sx、sy、s → 间距 / t → 命名区域 / Modifier.item → 放置、跨度和对齐")
                GalleryParagraph("显式坐标从 1 开始；0 表示自动选择，负数相对显式轨道末尾定位。Int 是弹性权重，Float 是 0f .. 1f 的百分比，Dp 是固定尺寸，Fr 是显式弹性单位。")

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

                GalleryParagraph("以上示例全部使用 Rachel 的 Grid 封装。官方 API 中的 flow 和范围形式的 area 尚未由当前 DSL 暴露；默认按行自动放置，隐式轨道使用 Auto，范围区域可以用 t 的起点与 xs / ys 表达。")
            }

            Component("OffsetBox") {
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

            Component("ActionScope") {
                ActionScope.Right.Container(modifier = Modifier.fillMaxWidth()) {
                    Icon(icon = Icons.Download, onClick = {})
                    Icon(icon = Icons.Upload, onClick = {})
                    Icon(icon = Icons.CloudDownload, onClick = {})
                    Icon(icon = Icons.CloudUpload, onClick = {})
                }
            }

            Component("HorizontalScrollContainer") {
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

            Component("Splitter") {
                GalleryParagraph("拖动中间的分隔条调整区域比例。子内容可以使用 fillMaxWidth 或 fillMaxHeight 填满各自区域，较小的内容按 alignment 对齐。")
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
            GalleryParagraph("定义两列三行，六个子项默认按行填充。拖动间距，比较 s(x, y) 与分别调用 sx / sy 的相同效果；s(value) 可同时设置两个方向。窄窗口可以横向滚动。")
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
            GalleryParagraph("只定义列，不定义行，也不为子项指定坐标。项目填满一行后自动新增 Auto 行；调整列数和项目数，观察隐式行随内容变化。")
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
            GalleryParagraph("固定轨道使用指定尺寸；百分比按扣除行间距后的可用高度计算；Auto 行适应内容；弹性轨道在内容最小尺寸的基础上分配剩余空间。这里可以在 10% .. 40% 之间调节百分比。")
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
            GalleryParagraph("三列使用相同英文文本：min 按最长单词确定最小内容宽度，max 按不换行的完整文本确定最大内容宽度，auto 在可用空间内适应内容。拖动网格宽度观察换行；英文空格让差异更容易辨认。")
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
            GalleryParagraph("第一行从明确的最小高度起分配，第二行从内容最小高度起分配，第三行固定高度。调整容器高度，观察两个弹性轨道在各自基础尺寸上，按相同权重分配剩余空间。")
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
            GalleryParagraph("中心项同时指定 x / y，末尾项用 -1 定位到最后一条显式行列；只指定 x 或 y 时，另一方向自动寻找空位。其余子项不设置坐标，自动避开已占用的位置。")
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
            GalleryParagraph("A 跨两行两列，右侧两个子项自动落位，页脚跨三列。A 保持较小尺寸时可以观察区域内对齐；开启填满区域后，子项填充整个跨度，对齐不再产生可见位移。")
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
            GalleryParagraph("参考官方经典卡片：宽窗口中，封面跨四行，文字放在右侧；窄窗口中，封面和文字改为单列。只改变轨道与 t 的区域映射，子项始终使用相同的 item(label)。")
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
            GalleryParagraph("两个命名区域各跨两列，并共享中间一列。默认后声明的 OVERLAY 绘制在上层；开启开关后，用 zIndex 将先声明的 BASE 提到前面。")
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
            GalleryParagraph("Grid 负责标题、列表和页脚的结构，LazyColumn 负责大量数据。列表所在的列与行都使用 minmax(0.dp, Fr(1f))，避开普通弹性轨道对 SubcomposeLayout 不支持的固有尺寸查询；Grid 同时提供有限高度，让列表独立滚动。")
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
