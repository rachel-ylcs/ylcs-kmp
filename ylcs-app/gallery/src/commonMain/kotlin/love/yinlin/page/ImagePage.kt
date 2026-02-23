package love.yinlin.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.node.condition
import love.yinlin.data.compose.Picture
import love.yinlin.gallery.resources.*

@Stable
object ImagePage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Icon") {
                ExampleRow {
                    Example("Normal") {
                        Icon(Icons.Home)
                    }

                    Example("Size") {
                        Icon(Icons.Home, modifier = Modifier.size(Theme.size.image8))
                    }

                    Example("Tip") {
                        Icon(Icons.Home, tip = "主页")
                    }

                    Example("Click") {
                        Icon(Icons.Home, onClick = { })
                    }

                    Example("LoadingIcon") {
                        LoadingIcon(
                            icon = Icons.Home,
                            animation = CircleLoading,
                            onClick = {
                                delay(2000)
                            }
                        )
                    }

                    Example("ColorIcon") {
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

            Component("NineGrid") {
                val imgRes = listOf(
                    Res.drawable.img0,
                    Res.drawable.img1,
                    Res.drawable.img2,
                    Res.drawable.img3
                )

                val imageBlock = @Composable { isSingle: Boolean, pic: Picture, onClick: () -> Unit ->
                    Image(
                        res = imgRes[pic.image.toInt()],
                        modifier = Modifier.condition(!isSingle) { fillMaxSize() }.clickable(onClick = onClick),
                        contentScale = if (isSingle) ContentScale.Inside else ContentScale.Crop,
                    )
                }

                ExampleRow {
                    Example("Single DefaultSize") {
                        NineGrid(pics = remember { listOf(Picture("0")) }, content = imageBlock)
                    }

                    Example("Single fillMaxWidth", modifier = Modifier.weight(1f)) {
                        NineGrid(
                            pics = remember { listOf(Picture("2")) },
                            modifier = Modifier.fillMaxWidth(),
                            content = imageBlock
                        )
                    }
                }

                ExampleRow {
                    Example("Single Portrait") {
                        NineGrid(pics = remember { listOf(Picture("1")) }, content = imageBlock)
                    }

                    Example("Video", modifier = Modifier.weight(1f)) {
                        NineGrid(
                            pics = remember { listOf(Picture("0", video = "0")) },
                            modifier = Modifier.fillMaxWidth(),
                            content = imageBlock
                        )
                    }
                }

                ExampleRow {
                    Example("2 Picture", modifier = Modifier.weight(1f)) {
                        NineGrid(
                            pics = remember { listOf(Picture("0"), Picture("1")) },
                            modifier = Modifier.fillMaxWidth(),
                            content = imageBlock
                        )
                    }
                    Example("3 Picture", modifier = Modifier.weight(1f)) {
                        NineGrid(
                            pics = remember { listOf(Picture("0"), Picture("1"), Picture("3")) },
                            modifier = Modifier.fillMaxWidth(),
                            content = imageBlock
                        )
                    }
                }

                ExampleRow {
                    Example("5 Picture, Fixed Width") {
                        NineGrid(
                            pics = remember { listOf(Picture("0"), Picture("1"), Picture("2"), Picture("3"), Picture("0")) },
                            modifier = Modifier.width(Theme.size.cell1),
                            content = imageBlock
                        )
                    }
                    Example("9 Picture", modifier = Modifier.weight(1f)) {
                        NineGrid(
                            pics = remember { listOf(Picture("0"), Picture("1"), Picture("2"), Picture("3"), Picture("0"), Picture("1"), Picture("2"), Picture("3"), Picture("0")) },
                            modifier = Modifier.fillMaxWidth(),
                            content = imageBlock
                        )
                    }
                }
            }
        }
    }
}