package love.yinlin.ui.screen.msg.douyin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling
import love.yinlin.AppModel
import love.yinlin.api.DouyinAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.douyin.DouyinVideo
import love.yinlin.extension.Object
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.filenameOrRandom
import love.yinlin.extension.parseJson
import love.yinlin.extension.rememberIntState
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Picker
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.platform.HeadlessBrowser
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.dialog.FloatingDownloadDialog
import love.yinlin.ui.screen.common.ScreenVideo

@Stable
class ScreenDouyin(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)
    private var items by mutableStateOf(emptyList<DouyinVideo>())
    private val gridState = LazyStaggeredGridState()
    private val browser = object : HeadlessBrowser() {
        override fun onUrlIntercepted(url: String): Boolean = url.contains("aweme/v1/web/aweme/post/")

        override fun onRequestIntercepted(url: String, response: String) {
            launch {
                state = catchingDefault(BoxState.NETWORK_ERROR) {
                    val json = response.parseJson
                    items = Coroutines.cpu { DouyinAPI.getDouyinVideos(json.Object) }
                    if (items.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
                }
                destroy()
            }
        }
    }

    @Composable
    private fun DouyinCard(
        item: DouyinVideo,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            shadowElevation = ThemeValue.Shadow.Surface
        ) {
            var videoIndex by rememberIntState(item) { item.videoUrl.lastIndex }

            Column(
                modifier = Modifier.fillMaxWidth().clickable {
                    item.videoUrl.getOrNull(videoIndex)?.let { url ->
                        navigate(ScreenVideo.Args(url = url))
                    }
                }
            ) {
                WebImage(
                    uri = item.picUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = item.title,
                    color = if (item.isTop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)
                )
                Text(
                    text = item.createTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RachelText(
                        text = item.likeNum.toString(),
                        icon = Icons.Outlined.Favorite,
                        style = MaterialTheme.typography.bodySmall,
                        padding = ThemeValue.Padding.ZeroValue,
                        modifier = Modifier.weight(1f)
                    )
                    RachelText(
                        text = item.commentNum.toString(),
                        icon = Icons.AutoMirrored.Outlined.Comment,
                        style = MaterialTheme.typography.bodySmall,
                        padding = ThemeValue.Padding.ZeroValue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RachelText(
                        text = item.collectNum.toString(),
                        icon = Icons.Outlined.Star,
                        style = MaterialTheme.typography.bodySmall,
                        padding = ThemeValue.Padding.ZeroValue,
                        modifier = Modifier.weight(1f)
                    )
                    RachelText(
                        text = item.shareNum.toString(),
                        icon = Icons.Outlined.Share,
                        style = MaterialTheme.typography.bodySmall,
                        padding = ThemeValue.Padding.ZeroValue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "切换视频源[${videoIndex + 1}]",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f).clickable {
                            videoIndex = (videoIndex + 1) % item.videoUrl.size
                        }.padding(ThemeValue.Padding.LittleValue)
                    )
                    ClickIcon(
                        icon = Icons.Outlined.Download,
                        onClick = {
                            item.videoUrl.getOrNull(videoIndex)?.let { url ->
                                val filename = url.filenameOrRandom(".mp4")
                                launch {
                                    Coroutines.io {
                                        Picker.prepareSaveVideo(filename)?.let { (origin, sink) ->
                                            val result = downloadVideoDialog.openSuspend(url, sink) { Picker.actualSave(filename, origin, sink) }
                                            Picker.cleanSave(origin, result)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override val title: String = "抖音"

    override suspend fun initialize() {
        state = BoxState.LOADING
        browser.load("https://www.douyin.com/user/MS4wLjABAAAATAf7yHksdW6CBPSjl9CW8k3c_x_drbwg0CVLTowlwzE")
    }

    override fun finalize() {
        browser.destroy()
    }

    @Composable
    override fun SubContent(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            bindPauseLoadWhenScrolling(gridState)
            PaginationStaggeredGrid(
                items = items,
                key = { it.id },
                columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CellWidth),
                state = gridState,
                canRefresh = false,
                canLoading = false,
                modifier = Modifier.fillMaxSize(),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalItemSpacing = ThemeValue.Padding.EqualSpace
            ) {
                DouyinCard(
                    item = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector? get() = if (isScrollTop) null else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        gridState.animateScrollToItem(0)
    }

    private val downloadVideoDialog = FloatingDownloadDialog()

    @Composable
    override fun Floating() {
        downloadVideoDialog.Land()
    }
}