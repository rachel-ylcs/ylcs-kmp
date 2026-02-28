package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.json.JsonObject
import love.yinlin.app
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberIntState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.HeadlessWebView
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.floating.downloadVideo
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.data.douyin.DouyinVideo
import love.yinlin.extension.Object
import love.yinlin.extension.parseJson
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import love.yinlin.tpl.DouyinAPI

@Stable
class ScreenDouyin : Screen() {
    private val provider = RachelStatefulProvider()
    private var items by mutableRefStateOf(emptyList<DouyinVideo>())
    private val gridState = LazyStaggeredGridState()

    override val title: String = "抖音"

    override suspend fun initialize() {
        if (platform == Platform.Android) {
            provider.withLoading {
                val json = Coroutines.sync<JsonObject> { future ->
                    val browser = object : HeadlessWebView(app.context) {
                        override fun onUrlIntercepted(url: String): Boolean = url.contains("aweme/v1/web/aweme/post/")
                        override fun onRequestIntercepted(url: String, response: String): Boolean {
                            future.send { response.parseJson.Object }
                            destroy()
                            return onUrlIntercepted(url)
                        }
                    }
                    future.clean { browser.destroy() }
                    browser.load("https://www.douyin.com/user/MS4wLjABAAAATAf7yHksdW6CBPSjl9CW8k3c_x_drbwg0CVLTowlwzE")
                }!!
                items = DouyinAPI.getDouyinVideos(json)
                items.isNotEmpty()
            }
        }
    }

    @Composable
    private fun DouyinCard(item: DouyinVideo, modifier: Modifier = Modifier) {
        var videoIndex by rememberIntState(item) { item.videoUrl.lastIndex }

        Surface(
            modifier = modifier,
            shape = Theme.shape.v5,
            shadowElevation = Theme.shadow.v3,
            onClick = {
                item.videoUrl.getOrNull(videoIndex)?.let { url ->
                    navigate(::ScreenVideo, url)
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                WebImage(
                    uri = item.picUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = item.title,
                    color = if (item.isTop) Theme.color.primary else LocalColor.current,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)
                )
                SimpleEllipsisText(
                    text = item.createTime,
                    style = Theme.typography.v8,
                    color = LocalColorVariant.current,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Favorite, modifier = Modifier.idIcon())
                        SimpleClipText(text = item.likeNum.toString(), style = Theme.typography.v8, modifier = Modifier.idText())
                    }
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                        SimpleClipText(text = item.commentNum.toString(), style = Theme.typography.v8, modifier = Modifier.idText())
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Star, modifier = Modifier.idIcon())
                        SimpleClipText(text = item.collectNum.toString(), style = Theme.typography.v8, modifier = Modifier.idText())
                    }
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Share, modifier = Modifier.idIcon())
                        SimpleClipText(text = item.shareNum.toString(), style = Theme.typography.v8, modifier = Modifier.idText())
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(
                        text = "切换视频源[${videoIndex + 1}]",
                        color = Theme.color.secondary,
                        modifier = Modifier.weight(1f).clickable {
                            videoIndex = (videoIndex + 1) % item.videoUrl.size
                        }
                    )
                    Icon(icon = Icons.Download, onClick = {
                        item.videoUrl.getOrNull(videoIndex)?.let { url ->
                            launch(ioContext) {
                                downloadVideoDialog.downloadVideo(url)
                            }
                        }
                    })
                }
            }
        }
    }

    @Composable
    override fun Content() {
        if (platform == Platform.Android) {
            StatefulBox(
                provider = provider,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                PaginationStaggeredGrid(
                    items = items,
                    key = { it.id },
                    columns = StaggeredGridCells.Adaptive(Theme.size.cell4),
                    state = gridState,
                    canRefresh = false,
                    canLoading = false,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Theme.padding.eValue,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                    verticalItemSpacing = Theme.padding.e
                ) {
                    DouyinCard(item = it, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        else UnsupportedPlatformComponent(modifier = Modifier.fillMaxSize())
    }

    override val fab: FAB = object : FAB() {
        private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

        override val action: FABAction? by derivedStateOf {
            if (platform == Platform.Android && isScrollTop) {
                FABAction(
                    iconProvider = { Icons.ArrowUpward },
                    onClick = { gridState.animateScrollToItem(0) }
                )
            } else null
        }
    }

    private val downloadVideoDialog = this land DialogDownload()
}