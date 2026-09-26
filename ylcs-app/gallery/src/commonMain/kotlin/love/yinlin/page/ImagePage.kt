package love.yinlin.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import love.yinlin.data.compose.Picture
import love.yinlin.gallery.resources.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

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
                                delay(2.seconds)
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
                val imgRes = [
                    Res.drawable.img0,
                    Res.drawable.img1,
                    Res.drawable.img2,
                    Res.drawable.img3
                ]

                val imageBlock = @Composable { contentScale: ContentScale, pic: Picture, onClick: () -> Unit ->
                    Image(
                        res = imgRes[pic.image.toInt()],
                        modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                        contentScale = contentScale
                    )
                }

                val buildPictures = { num: Int ->
                    List(num) { Picture(Random.nextInt(0, 4).toString()) }
                }

                ExampleRow {
                    Example("p1 Picture", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { [Picture("0")] }, content = imageBlock)
                    }
                    Example("p1 Video", modifier = Modifier.weight(1f)) {
                        NineGrid(modifier = Modifier.fillMaxWidth(), pics = remember { [Picture("0", video = "0")] }, content = imageBlock)
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
                }
            }
        }
    }
}