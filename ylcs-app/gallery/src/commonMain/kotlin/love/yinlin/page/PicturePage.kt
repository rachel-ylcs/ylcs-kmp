package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import love.yinlin.GalleryActions
import love.yinlin.GalleryCaption
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberNull
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.rememberPalette
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.text.Text
import love.yinlin.data.Picture
import love.yinlin.gallery.resources.*
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Stable
object PicturePage : Page(
    id = "picture",
    icon = Icons.GridOn,
    title = "图片",
    english = "Picture",
    description = "图片处理或布局",
    keywords = "图标 配图 九宫格 调色板",
    group = PageGroup.Foundation,
) {
    private val demoIcon = component("Icon", "用图标表示操作或状态，支持提示和自定义颜色。", "LoadingIcon ColorIcon")
    private val demoNineGrid = component("NineGrid", "把照片和视频封面排成九宫格。", "配图 视频")
    private val demoPalette = component("Palette", "从 ImageBitmap 异步生成并缓存调色板状态。", "ImageBitmap 图片 主色 调色板", "palette")

    @Stable
    abstract class TagPicture(val tag: Int) : Picture {
        final override val image: String = tag.toString()
    }

    @Stable
    class TagImage(tag: Int) : TagPicture(tag) {
        override val isVideo: Boolean = false
    }

    @Stable
    class TagVideo(tag: Int) : TagPicture(tag) {
        override val isVideo: Boolean = true
    }

    private fun Int.toPaletteHex(): String = "#${toUInt().toString(16).takeLast(6).uppercase()}"

    @Composable
    private fun IconSample(title: String, content: @Composable () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
        ) {
            Box(Modifier.heightIn(min = Theme.size.image8), contentAlignment = Alignment.Center) {
                content()
            }
            GalleryCaption(title)
        }
    }

    @Composable
    override fun Content() {
        ComponentColumn {
            Component(demoIcon) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Theme.color.backgroundVariant.copy(alpha = 0.45f), Theme.shape.v3)
                        .padding(Theme.padding.eValue6),
                ) {
                    GalleryActions {
                        IconSample("Normal") {
                            Icon(Icons.Home)
                        }
                        IconSample("Size") {
                            Icon(Icons.Home, modifier = Modifier.size(Theme.size.image8))
                        }
                        IconSample("Tip") {
                            Icon(Icons.Home, tip = "主页")
                        }
                        IconSample("Click") {
                            Icon(Icons.Home, onClick = { })
                        }
                        IconSample("LoadingIcon") {
                            LoadingIcon(
                                icon = Icons.Home,
                                animation = CircleLoading,
                                onClick = {
                                    delay(2.seconds)
                                }
                            )
                        }
                        IconSample("ColorIcon") {
                            Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
                                ColorIcon(
                                    icon = Icons.Home,
                                    color = Theme.color.onContainer,
                                    background = Theme.color.primaryContainer,
                                )
                                ColorIcon(
                                    icon = Icons.Home,
                                    color = Theme.color.onContainer,
                                    background = Theme.color.secondaryContainer,
                                )
                                ColorIcon(
                                    icon = Icons.Home,
                                    color = Theme.color.onContainer,
                                    background = Theme.color.tertiaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            Component(demoNineGrid) {
                val imgRes = [
                    Res.drawable.img0,
                    Res.drawable.img1,
                    Res.drawable.img2,
                    Res.drawable.img3
                ]

                val imageBlock = @Composable { contentScale: ContentScale, pic: TagPicture, onClick: () -> Unit ->
                    Image(
                        res = imgRes[pic.tag],
                        modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                        contentScale = contentScale
                    )
                }

                val buildPictures = { num: Int ->
                    List(num) { TagImage(Random.nextInt(0, 4)) }
                }

                ExampleRow {
                    Example("p1 Picture", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { [TagImage(0)] }, content = imageBlock)
                    }
                    Example("p1 Video", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { [TagVideo(0)] }, content = imageBlock)
                    }
                }

                ExampleRow {
                    Example("p2", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(2) }, content = imageBlock)
                    }
                    Example("p3", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(3) }, content = imageBlock)
                    }
                }

                ExampleRow {
                    Example("p4", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(4) }, content = imageBlock)
                    }
                    Example("p5", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(5) }, content = imageBlock)
                    }
                }

                ExampleRow {
                    Example("p6", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(6) }, content = imageBlock)
                    }
                    Example("p7", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(7) }, content = imageBlock)
                    }
                }

                ExampleRow {
                    Example("p8", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(8) }, content = imageBlock)
                    }
                    Example("p9", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(9) }, content = imageBlock)
                    }
                    Example("p12", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { buildPictures(12) }, content = imageBlock)
                    }
                }
            }

            Component(demoPalette) {
                var bitmap: ImageBitmap? by rememberNull()

                LaunchedEffect(Unit) {
                    bitmap = requireNotNull(ImageBitmap.decode(getDrawableResourceBytes(getSystemResourceEnvironment(), Res.drawable.img0)))
                }

                val source = bitmap
                if (source == null) {
                    Text("正在加载示例图像…", color = Theme.color.onBackgroundVariant)
                }
                else {
                    val palette = rememberPalette(source)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v8)
                    ) {
                        Image(
                            bitmap = source,
                            modifier = Modifier.widthIn(max = Theme.size.cell1 * 1.5f).fillMaxWidth().aspectRatio(1.77778f).clip(Theme.shape.v3),
                            contentScale = ContentScale.Crop
                        )

                        Text("默认调色板", color = Theme.color.onBackground)
                        when {
                            palette.isLoading -> Text("正在分析 ImageBitmap...", color = Theme.color.onBackgroundVariant)
                            palette.error != null -> Text("取色失败：${palette.error?.message}", color = Theme.color.error)
                            !palette.isReady -> Text("没有可用颜色", color = Theme.color.onBackgroundVariant)
                            else -> {
                                val dominant = palette.dominantSwatch
                                if (dominant != null) {
                                    Text("主色 ${dominant.rgb.toPaletteHex()} · ${dominant.population} 像素")
                                    Column(modifier = Modifier.fillMaxWidth().clip(Theme.shape.v3).background(dominant.color).padding(Theme.padding.v8)) {
                                        Text("标题文字", color = dominant.titleColor)
                                        Text("正文文字", color = dominant.bodyColor)
                                    }
                                }

                                Text("六种标准目标色")
                                val targetSwatches = [
                                    palette.vibrantSwatch?.let { "Vibrant" to it },
                                    palette.lightVibrantSwatch?.let { "Light vibrant" to it },
                                    palette.darkVibrantSwatch?.let { "Dark vibrant" to it },
                                    palette.mutedSwatch?.let { "Muted" to it },
                                    palette.lightMutedSwatch?.let { "Light muted" to it },
                                    palette.darkMutedSwatch?.let { "Dark muted" to it },
                                ]
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                                ) {
                                    for (swatch in targetSwatches) {
                                        val [label, swatch] = swatch ?: continue

                                        Column(
                                            modifier = Modifier.width(Theme.size.image4),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(Modifier.fillMaxWidth().height(44.dp).clip(Theme.shape.v3).background(swatch.color))
                                            Text(label)
                                            Text(swatch.rgb.toPaletteHex(), color = Theme.color.onBackgroundVariant)
                                        }
                                    }
                                }

                                Text("全部 Swatch · ${palette.swatches.size} 种颜色")
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e)
                                ) {
                                    for (swatch in palette.swatches) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(Modifier.size(30.dp).clip(Theme.shape.v3).background(swatch.color))
                                            Text(swatch.population.toString(), color = Theme.color.onBackgroundVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
