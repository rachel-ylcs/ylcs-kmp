package love.yinlin.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.Serializable
import love.yinlin.api.WeiboAPI
import love.yinlin.compose.*
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.extension.DateEx
import love.yinlin.extension.filenameOrRandom
import love.yinlin.platform.*
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.LoadingBox
import love.yinlin.compose.ui.layout.SimpleEmptyBox
import love.yinlin.compose.ui.layout.SimpleLoadingBox
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.service
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.screen.dialog.FloatingDownloadDialog

@Composable
private fun UserInfoCard(
    user: WeiboUser,
    isFollowed: Boolean,
    onFollowClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(CustomTheme.padding.extraValue),
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
    ) {
        OffsetLayout(y = -CustomTheme.size.mediumImage / 3) {
            WebImage(
                uri = user.info.avatar,
                key = remember { DateEx.TodayString },
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.size(CustomTheme.size.mediumImage)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.info.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ClickIcon(
                    icon = if (isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    tip = if (isFollowed) "取消关注" else "关注",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onFollowClick(!isFollowed) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
            ) {
                Text(
                    text = "关注 ${user.followNum}",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "粉丝 ${user.fansNum}",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UserAlbumItem(
    album: WeiboAlbum,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onAlbumClick),
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WebImage(
            uri = album.pic,
            modifier = Modifier.size(CustomTheme.size.image)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            Text(
                text = album.title,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = album.num,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.time,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Stable
class ScreenWeiboUser(manager: ScreenManager, private val args: Args) : Screen<ScreenWeiboUser.Args>(manager) {
    @Stable
    @Serializable
    data class Args(val id: String)

    private val subScreenMsg = manager.get<ScreenMain>().get<SubScreenMsg>()

    private var state by mutableStateOf(BoxState.EMPTY)
    private var items by mutableRefStateOf(emptyList<Weibo>())
    private val listState = LazyStaggeredGridState()
    private var user: WeiboUser? by mutableRefStateOf(null)
    private var albums: List<WeiboAlbum>? by mutableRefStateOf(null)

    private fun onFollowClick(user: WeiboUser, isFollow: Boolean) {
        val weiboUsers = service.config.weiboUsers
        if (isFollow) {
            if (!weiboUsers.contains { it.id == user.info.id }) weiboUsers += user.info
        }
        else weiboUsers.removeAll { it.id == user.info.id }
    }

    private fun onAlbumClick(album: WeiboAlbum) {
        navigate(ScreenWeiboAlbum.Args(album.containerId, album.title))
    }

    private fun onPicturesDownload(pics: List<Picture>) {
        Platform.use(
            *Platform.Phone,
            ifTrue = {
                launch {
                    slot.loading.openSuspend()
                    Coroutines.io {
                        for (pic in pics) {
                            val url = pic.source
                            val filename = url.filenameOrRandom(".webp")
                            Picker.prepareSavePicture(filename)?.let { (origin, sink) ->
                                val result = sink.use {
                                    val result = NetClient.file.safeDownload(
                                        url = url,
                                        sink = it,
                                        isCancel = { false },
                                        onGetSize = {},
                                        onTick = { _, _ -> }
                                    )
                                    if (result) Picker.actualSave(filename, origin, sink)
                                    result
                                }
                                Picker.cleanSave(origin, result)
                            }
                        }
                    }
                    slot.loading.close()
                }
            },
            ifFalse = {
                slot.tip.warning(UnsupportedPlatformText)
            }
        )
    }

    private fun onVideoDownload(url: String) {
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

    @Composable
    private fun Portrait(
        user: WeiboUser,
        albums: List<WeiboAlbum>?
    ) {
        LazyColumn(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            item(key = ItemKey("UserInfoCard")) {
                WebImage(
                    uri = user.background,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f),
                    contentScale = ContentScale.Crop,
                    alpha = 0.8f
                )
                UserInfoCard(
                    user = user,
                    isFollowed = service.config.weiboUsers.contains { it.id == user.info.id },
                    onFollowClick = { onFollowClick(user, it) },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(CustomTheme.padding.equalValue))
            }
            if (albums != null) {
                items(
                    items = albums,
                    key = { it.containerId }
                ) {
                    UserAlbumItem(
                        album = it,
                        onAlbumClick = { onAlbumClick(it) },
                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                    )
                }
            }
            item(key = ItemKey("Text")) {
                Text(
                    text = "最新微博",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                )
            }
            items(
                items = items,
                key = { it.id }
            ) { weibo ->
                WeiboCard(
                    weibo = weibo,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                    onPicturesDownload = ::onPicturesDownload,
                    onVideoDownload = ::onVideoDownload
                )
            }
        }
    }

    @Composable
    private fun Landscape(
        user: WeiboUser,
        albums: List<WeiboAlbum>?
    ) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Surface(
                modifier = Modifier.width(CustomTheme.size.panelWidth).fillMaxHeight(),
                shadowElevation = CustomTheme.shadow.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    WebImage(
                        uri = user.background,
                        key = remember { DateEx.TodayString },
                        modifier = Modifier.fillMaxWidth().aspectRatio(2f),
                        contentScale = ContentScale.Crop,
                        alpha = 0.8f
                    )
                    UserInfoCard(
                        user = user,
                        isFollowed = service.config.weiboUsers.contains { it.id == user.info.id },
                        onFollowClick = { onFollowClick(user, it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = CustomTheme.padding.horizontalSpace))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = CustomTheme.padding.verticalSpace)) {
                        if (albums == null) SimpleLoadingBox()
                        else {
                            if (albums.isEmpty()) SimpleEmptyBox()
                            else LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = CustomTheme.padding.equalValue,
                                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                            ) {
                                items(
                                    items = albums,
                                    key = { it.containerId }
                                ) {
                                    UserAlbumItem(
                                        album = it,
                                        onAlbumClick = { onAlbumClick(it) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Space()
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shadowElevation = CustomTheme.shadow.surface
            ) {
                StatefulBox(
                    state = state,
                    modifier = Modifier.fillMaxSize()
                ) {
                    WeiboGrid(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        items = items,
                        onPicturesDownload = ::onPicturesDownload,
                        onVideoDownload = ::onVideoDownload
                    )
                }
            }
        }
    }

    override suspend fun initialize() {
        launch {
            val data = WeiboAPI.getWeiboUser(args.id)
            user = if (data is Data.Success) data.data else null
            user?.info?.id?.let { id ->
                if (state != BoxState.LOADING) {
                    state = BoxState.LOADING
                    val newItems = mutableMapOf<String, Weibo>()
                    val result = WeiboAPI.getUserWeibo(id)
                    if (result is Data.Success) newItems += result.data.associateBy { it.id }
                    items = newItems.map { it.value }.sortedDescending()
                    state = if (newItems.isEmpty()) BoxState.NETWORK_ERROR else BoxState.CONTENT
                }
            }
        }
        launch {
            val data = WeiboAPI.getWeiboUserAlbum(args.id)
            albums = if (data is Data.Success) data.data else null
        }
    }

    override val title: String by derivedStateOf { user?.info?.name ?: "" }

    @Composable
    override fun Content(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides subScreenMsg.processor) {
            user?.let {
                when (device.type) {
                    Device.Type.PORTRAIT -> Portrait(user = it, albums = albums)
                    Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(user = it, albums = albums)
                }
            } ?: LoadingBox()
        }
    }

    private val downloadVideoDialog = FloatingDownloadDialog()

    @Composable
    override fun Floating() {
        downloadVideoDialog.Land()
    }
}