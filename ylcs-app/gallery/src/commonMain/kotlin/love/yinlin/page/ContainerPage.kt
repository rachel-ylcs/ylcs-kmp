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
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberNull
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.container.*
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderDpConverter
import love.yinlin.compose.ui.input.SliderFloatConverter
import love.yinlin.compose.ui.input.SliderIntConverter
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.enum
import love.yinlin.extension.enumSize
import love.yinlin.gallery.resources.*

@Stable
object ContainerPage : Page(
    id = "container",
    icon = Icons.Package,
    title = "容器",
    english = "Container",
    description = "组织内容、呈现加载状态，也可添加轮播和水印。",
    keywords = "内容 卡片",
    group = PageGroup.Layout,
) {
    private val demoSurface = component("Surface", "为内容提供带圆角、边框或阴影的表面。", "卡片")
    private val demoStatefulBox = component("StatefulBox", "按加载结果显示内容、空态或错误提示。", "加载 空态")
    private val demoReplaceableBox = component("ReplaceableBox", "在同一位置添加、替换或删除一个内容。", "替换")
    private val demoAdderBox = component("AdderBox", "向容器中添加项目，也可替换或删除已有项目。", "添加")
    private val demoBanner = component("Banner", "自动轮播一组图片，也可以手动切换。", "轮播")
    private val demoWatermark = component("Watermark", "在内容上平铺水印，仍可操作下方控件。", "平铺 透明度")

    @Composable
    override fun Content() {
        ComponentColumn {
            Component(demoSurface) {
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

            Component(demoStatefulBox) {
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

            Component(demoReplaceableBox) {
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

            Component(demoAdderBox) {
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

            Component(demoBanner) {
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
            Component(demoWatermark) {
                var angle by rememberValueState(30f)
                var alpha by rememberValueState(0.12f)
                var space by rememberValueState(48.dp)
                var clicks by rememberValueState(0)

                ExampleRow(maxItemsInEachRow = 3) {
                    Example("逆时针角度 · ${angle.toInt()}°", Modifier.weight(1f)) {
                        Slider(angle, SliderFloatConverter(-60f, 60f), { angle = it }, onValueChange = { angle = it }, modifier = Modifier.fillMaxWidth())
                    }
                    Example("不透明度 · ${(alpha * 100).toInt()}%", Modifier.weight(1f)) {
                        Slider(alpha, SliderFloatConverter(0f, 0.4f), { alpha = it }, onValueChange = { alpha = it }, modifier = Modifier.fillMaxWidth())
                    }
                    Example("标志间距 · ${space.value.toInt()}dp", Modifier.weight(1f)) {
                        Slider(space, SliderDpConverter(8.dp, 96.dp), { space = it }, onValueChange = { space = it }, modifier = Modifier.fillMaxWidth())
                    }
                }

                Watermark(
                    mark = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.g)) {
                            Icon(icon = Icons.VerifiedUser, color = Theme.color.primary)
                            Text("RACHEL UI", color = Theme.color.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(Theme.size.cell3).clip(Theme.shape.v3).background(Theme.color.backgroundVariant),
                    angle = angle,
                    alpha = alpha,
                    space = space,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(Theme.padding.v8),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.g)) {
                            Text("内容始终清晰且可交互", color = Theme.color.onBackground)
                            Text("水印只参与绘制，不遮挡下面的内容。", color = Theme.color.onBackgroundVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.g)) {
                            PrimaryTextButton("点击测试", onClick = { clicks++ })
                            Text("已点击 $clicks 次", color = Theme.color.onBackground)
                        }
                    }
                }
            }
        }
    }
}