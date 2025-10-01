package love.yinlin.ui.screen.msg.pictures

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
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.photo.PhotoAlbum
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.FloatingDialogInput
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
class ScreenPictures(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private var keyword: String? = null
    private val page = object : PaginationArgs<PhotoAlbum, Int, String, Int>("2099-12-31", Int.MAX_VALUE) {
        override fun distinctValue(item: PhotoAlbum): Int = item.aid
        override fun offset(item: PhotoAlbum): String = item.ts
        override fun arg1(item: PhotoAlbum): Int = item.aid
    }

    private val listState = LazyListState()

    private suspend fun requestNewPhotos() {
        if (state != BoxState.LOADING) {
            state = BoxState.LOADING
            val result = ClientAPI.request(
                route = API.Common.Photo.SearchPhotoAlbums,
                data = API.Common.Photo.SearchPhotoAlbums.Request(
                    keyword = keyword,
                    num = page.pageNum
                )
            )
            state = if (result is Data.Success) {
                if (page.newData(result.data)) BoxState.CONTENT else BoxState.EMPTY
            } else BoxState.NETWORK_ERROR
        }
    }

    private suspend fun requestMorePhotos() {
        val result = ClientAPI.request(
            route = API.Common.Photo.SearchPhotoAlbums,
            data = API.Common.Photo.SearchPhotoAlbums.Request(
                keyword = keyword,
                ts = page.offset,
                aid = page.arg1,
                num = page.pageNum,
            )
        )
        if (result is Data.Success) page.moreData(result.data)
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
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
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
                    preferredItemWidth = ThemeValue.Size.CellWidth,
                    itemSpacing = ThemeValue.Padding.HorizontalSpace,
                    modifier = Modifier.fillMaxWidth().clipToBounds()
                ) { index ->
                    WebImage(
                        uri = album.thumbPath(index),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(ThemeValue.Size.CellWidth)
                            .aspectRatio(0.66667f)
                            .maskClip(MaterialTheme.shapes.large),
                        onClick = {
                            val pics = List(album.picNum) {
                                Picture(album.thumbPath(it), album.picPath(it))
                            }
                            navigate(ScreenImagePreview.Args(pics, index))
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
    override fun SubContent(device: Device) {
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
                itemDivider = ThemeValue.Padding.Value,
                modifier = Modifier.fillMaxSize()
            ) {
                PhotoAlbumLayout(
                    album = it,
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue)
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch {
            keyword = null
            requestNewPhotos()
        }
        else listState.animateScrollToItem(0)
    }

    private val searchDialog = FloatingDialogInput(
        hint = "关键字/地点/作者",
        maxLength = 16
    )

    @Composable
    override fun Floating() {
        searchDialog.Land()
    }
}