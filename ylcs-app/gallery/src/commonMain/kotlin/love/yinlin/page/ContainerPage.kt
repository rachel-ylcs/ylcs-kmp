package love.yinlin.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberNull
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.graphics.HSV
import love.yinlin.compose.ui.container.*
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderDpConverter
import love.yinlin.compose.ui.input.SliderIntConverter
import love.yinlin.compose.ui.text.Text
import love.yinlin.gallery.resources.*

@Stable
object ContainerPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Surface") {
                var padding by rememberValueState(0.dp)
                var shape by rememberValueState(0.dp)
                var shadow by rememberValueState(0.dp)
                var tonal by rememberValueState(0)
                var border by rememberValueState(0.dp)

                ExampleRow {
                    Text(text = "边距")
                    Slider(padding, SliderDpConverter(0.dp, 40.dp), { padding = it }, onValueChange = { padding = it })
                    Text(text = "形状")
                    Slider(shape, SliderDpConverter(0.dp, 40.dp), { shape = it }, onValueChange = { shape = it })
                    Text(text = "阴影")
                    Slider(shadow, SliderDpConverter(0.dp, 20.dp), { shadow = it }, onValueChange = { shadow = it })
                    Text(text = "色调")
                    Slider(tonal, SliderIntConverter(0, 10), { tonal = it }, onValueChange = { tonal = it })
                    Text(text = "边框")
                    Slider(border, SliderDpConverter(0.dp, 10.dp), { border = it }, onValueChange = { border = it })
                }

                Box(
                    modifier = Modifier.size(Theme.size.cell3, Theme.size.cell4)
                        .background(Theme.color.secondaryContainer)
                        .padding(Theme.padding.v8),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(padding),
                        shape = RoundedCornerShape(shape),
                        shadowElevation = shadow,
                        tonalLevel = tonal,
                        border = if (border == 0.dp) null else BorderStroke(border, Theme.color.tertiary)
                    ) {
                        Text(
                            text = "hello world",
                            color = Theme.color.onContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxSize().background(Theme.color.primaryContainer)
                        )
                    }
                }
            }

            Component("OffsetBox") {
                var x by rememberValueState(0.5f)
                var y by rememberValueState(0.5f)
                ExampleRow {
                    Text(text = "x")
                    Slider(x, { x = it }, onValueChange = { x = it })
                    Text(text = "y")
                    Slider(y, { y = it }, onValueChange = { y = it })
                    Box(modifier = Modifier.weight(1f))
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

            Component("StatefulBox") {
                val provider = remember { DefaultStatefulProvider() }

                Filter(
                    size = StatefulStatus.entries.size,
                    selectedProvider = { provider.status.ordinal == it },
                    titleProvider = { StatefulStatus.entries[it].name },
                    onClick = { index, selected -> if (selected) provider.status = StatefulStatus.entries[index] }
                )

                ThemeContainer {
                    StatefulBox(provider = provider, modifier = Modifier.size(Theme.size.cell3).background(Theme.color.primaryContainer)) {
                        Text("Hello world")
                    }
                }
            }

            Component("ReplaceableBox") {
                val colorList = listOf(
                    Theme.color.primaryContainer,
                    Theme.color.secondaryContainer,
                    Theme.color.tertiaryContainer
                )
                var index: Int? by rememberNull()

                ReplaceableBox(
                    value = index,
                    onReplace = { index = index?.let { (it + 1) % colorList.size } ?: 0 },
                    onDelete = { index = null },
                ) {
                    Box(
                        modifier = Modifier.size(Theme.size.cell4).background(colorList[it]),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "点我替换颜色\n长按删除", color = Theme.color.onContainer)
                    }
                }
            }

            Component("AdderBox") {
                val colorList = listOf(
                    Theme.color.primaryContainer,
                    Theme.color.secondaryContainer,
                    Theme.color.tertiaryContainer,
                    Theme.color.surface
                )
                val items = remember { mutableStateListOf<Color>() }

                AdderBox(
                    maxNum = 9,
                    items = items,
                    modifier = Modifier.width(Theme.size.cell1),
                    onAdd = { items += colorList.random() },
                    onReplace = { index, _ -> items[index] = colorList.random() },
                    onDelete = { index, _ -> items.removeAt(index) }
                ) { _, color ->
                    Box(modifier = Modifier.fillMaxSize().background(color))
                }
            }

            Component("Banner") {
                val imgRes = listOf(
                    Res.drawable.img0,
                    Res.drawable.img1,
                    Res.drawable.img2,
                    Res.drawable.img3
                )

                Banner(
                    size = imgRes.size,
                    interval = 3000L,
                    modifier = Modifier.size(Theme.size.cell1, Theme.size.cell3)
                ) { index ->
                    Image(
                        res = imgRes[index],
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
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

            Component("AdaptiveTwoBox") {
                AdaptiveTwoBox(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(Theme.size.cell1, Theme.size.cell2).background(Theme.color.primaryContainer))
                    Box(modifier = Modifier.size(Theme.size.cell2, Theme.size.cell3).background(Theme.color.secondaryContainer))
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
        }
    }
}