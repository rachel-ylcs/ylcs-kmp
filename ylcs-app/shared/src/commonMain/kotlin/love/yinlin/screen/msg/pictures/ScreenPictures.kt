package love.yinlin.screen.msg.pictures

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.data.Picture
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.data.rachel.photo.PhotoAlbum
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.*
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.cs.*
import love.yinlin.screen.common.ScreenImagePreview

@Stable
class ScreenPictures(manager: ScreenManager) : Screen(manager) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private var keyword: String? = null
    private val page = object : PaginationArgs<PhotoAlbum, Int, String, Int>(
        default = "2099-12-31",
        default1 = Int.MAX_VALUE,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: PhotoAlbum): Int = item.aid
        override fun offset(item: PhotoAlbum): String = item.ts
        override fun arg1(item: PhotoAlbum): Int = item.aid
    }

    private val listState = LazyListState()

    private suspend fun requestNewPhotos() {
        if (state != BoxState.LOADING) {
            state = BoxState.LOADING
            state = ApiPhotoSearchPhotoAlbums.requestNull(keyword, page.default, page.default1,page.pageNum)?.let {
                if (page.newData(it.o1)) BoxState.CONTENT else BoxState.EMPTY
            } ?: BoxState.NETWORK_ERROR
        }
    }

    private suspend fun requestMorePhotos() {
        ApiPhotoSearchPhotoAlbums.request(keyword, page.offset, page.arg1, page.pageNum) { page.moreData(it) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PhotoAlbumLayout(
        album: PhotoAlbum,
        modifier: Modifier = Modifier
    ) {
        if (album.picNum in 1 .. PhotoAlbum.MAX_NUM) {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = album.picNum.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SplitLayout(
                    modifier = Modifier.fillMaxWidth(),
                    left = {
                        Text(
                            text = "${album.ts}  ${album.location ?: ""}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    right = {
                        Text(
                            text = album.author ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                )
                HorizontalMultiBrowseCarousel(
                    state = rememberCarouselState { album.picNum },
                    preferredItemWidth = CustomTheme.size.cellWidth,
                    itemSpacing = CustomTheme.padding.horizontalSpace,
                    modifier = Modifier.fillMaxWidth().clipToBounds()
                ) { index ->
                    WebImage(
                        uri = remember(album) { album.thumbPath(index).url },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(CustomTheme.size.cellWidth)
                            .aspectRatio(0.66667f)
                            .maskClip(MaterialTheme.shapes.large),
                        onClick = {
                            val pics = List(album.picNum) {
                                Picture(album.thumbPath(it).url, album.picPath(it).url)
                            }
                            navigate(::ScreenImagePreview, pics, index)
                        }
                    )
                }
            }
        }
    }

    override val title: String = "美图"

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(Icons.Outlined.EmojiObjects, "分享") {
            slot.info.openSuspend(
                title = "图集分享",
                content = "欢迎大家分享相机线下拍摄的超清原图给其他小银子, 有贡献图集意愿的小银子可联系我们收录, 我们也会为您赠予银币奖励!"
            )
        }

        ActionSuspend(Icons.Outlined.Search, "搜索") {
            searchDialog.openSuspend()?.let { result ->
                keyword = result
                requestNewPhotos()
            }
        }
    }

    override suspend fun initialize() {
        keyword = null
        requestNewPhotos()
    }

    @Composable
    override fun Content(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationColumn(
                items = page.items,
                key = { it.aid },
                state = listState,
                canRefresh = false,
                canLoading = page.canLoading,
                onLoading = { requestMorePhotos() },
                itemDivider = CustomTheme.padding.value,
                modifier = Modifier.fillMaxSize()
            ) {
                PhotoAlbumLayout(
                    album = it,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue)
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector by derivedStateOf { if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward }

    override suspend fun onFabClick() {
        if (isScrollTop) launch {
            keyword = null
            requestNewPhotos()
        }
        else listState.animateScrollToItem(0)
    }

    private val searchDialog = this land FloatingDialogInput(hint = "关键字/地点/作者", maxLength = 16)
}