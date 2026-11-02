package love.yinlin.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberNull
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.container.*
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderDpConverter
import love.yinlin.compose.ui.input.SliderIntConverter
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.enum
import love.yinlin.extension.enumSize
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

                ExampleRow(maxItemsInEachRow = 3) {
                    Example("内边距 · $padding", Modifier.weight(1f)) {
                        Slider(padding, SliderDpConverter(0.dp, 40.dp), { padding = it }, onValueChange = { padding = it }, modifier = Modifier.fillMaxWidth())
                    }
                    Example("圆角 · $shape", Modifier.weight(1f)) {
                        Slider(shape, SliderDpConverter(0.dp, 40.dp), { shape = it }, onValueChange = { shape = it }, modifier = Modifier.fillMaxWidth())
                    }
                    Example("阴影 · $shadow", Modifier.weight(1f)) {
                        Slider(shadow, SliderDpConverter(0.dp, 20.dp), { shadow = it }, onValueChange = { shadow = it }, modifier = Modifier.fillMaxWidth())
                    }
                    Example("色调 · $tonal", Modifier.weight(1f)) {
                        Slider(tonal, SliderIntConverter(0, 10), { tonal = it }, onValueChange = { tonal = it }, modifier = Modifier.fillMaxWidth())
                    }
                    Example("边框 · $border", Modifier.weight(1f)) {
                        Slider(border, SliderDpConverter(0.dp, 10.dp), { border = it }, onValueChange = { border = it }, modifier = Modifier.fillMaxWidth())
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Theme.color.backgroundVariant).padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(Theme.size.cell3, Theme.size.cell4).background(Theme.color.secondaryContainer).padding(Theme.padding.v8)
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
            }

            Component("StatefulBox") {
                val provider = remember { DefaultStatefulProvider() }

                Filter(
                    size = enumSize<StatefulStatus>(),
                    selectedProvider = { provider.status.ordinal == it },
                    titleProvider = { enum<StatefulStatus>(it).name },
                    onClick = { index, selected -> if (selected) provider.status = enum(index) }
                )

                ThemeContainer {
                    StatefulBox(provider = provider, modifier = Modifier.size(Theme.size.cell3).background(Theme.color.primaryContainer)) {
                        Text("Hello world")
                    }
                }
            }

            Component("ReplaceableBox") {
                val colorList = [
                    Theme.color.primaryContainer,
                    Theme.color.secondaryContainer,
                    Theme.color.tertiaryContainer
                ]
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
                val colorList = [
                    Theme.color.primaryContainer,
                    Theme.color.secondaryContainer,
                    Theme.color.tertiaryContainer,
                    Theme.color.surface
                ]
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
                val imgRes = [
                    Res.drawable.img0,
                    Res.drawable.img1,
                    Res.drawable.img2,
                    Res.drawable.img3
                ]

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
        }
    }
}
