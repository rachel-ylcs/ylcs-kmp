package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.common.WeiboCard
import love.yinlin.compose.ui.common.WeiboGrid
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.extension.DateEx
import love.yinlin.tpl.WeiboAPI

@Stable
class ScreenWeiboUser(private val userId: String) : Screen() {
    private val provider = RachelStatefulProvider()
    private var items by mutableRefStateOf(emptyList<Weibo>())
    private val listState = LazyStaggeredGridState()
    private var currentUser: WeiboUser? by mutableRefStateOf(null)
    private var albums: List<WeiboAlbum>? by mutableRefStateOf(null)

    private fun onFollowClick(user: WeiboUser, isFollow: Boolean) {
        val weiboUsers = app.config.weiboUsers
        if (isFollow) {
            if (!weiboUsers.contains { it.id == user.info.id }) weiboUsers += user.info
        }
        else weiboUsers.removeAll { it.id == user.info.id }
    }

    private fun onAlbumClick(album: WeiboAlbum) {
        navigate(::ScreenWeiboAlbum, album.containerId, album.title)
    }

    override val title: String get() = currentUser?.info?.name ?: ""

    override suspend fun initialize() {
        supervisorScope {
            this.launch {
                currentUser = WeiboAPI.getWeiboUser(userId)
                currentUser?.info?.id?.let { id ->
                    provider.withLoading {
                        val newItems = mutableMapOf<String, Weibo>()
                        newItems += WeiboAPI.getUserWeibo(id)!!.associateBy { it.id }
                        items = newItems.map { it.value }.sortedDescending()
                        newItems.isNotEmpty()
                    }
                }
            }
            this.launch {
                albums = WeiboAPI.getWeiboUserAlbum(userId)
            }
        }
    }

    @Composable
    private fun UserInfoCard(
        user: WeiboUser,
        isFollowed: Boolean,
        onFollowClick: (Boolean) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
        ) {
            WebImage(
                uri = user.info.avatar,
                key = remember { DateEx.TodayString },
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.size(Theme.size.image8)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(
                        text = user.info.name,
                        style = Theme.typography.v7.bold,
                        color = Theme.color.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        icon = if (isFollowed) Icons.Favorite else Icons.FavoriteBorder,
                        tip = if (isFollowed) "取消关注" else "关注",
                        color = Theme.color.primary,
                        onClick = { onFollowClick(!isFollowed) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SimpleEllipsisText(text = "关注 ${user.followNum}")
                    SimpleEllipsisText(text = "粉丝 ${user.fansNum}")
                }
            }
        }
    }

    @Composable
    private fun UserInfoLayout(user: WeiboUser) {
        WebImage(
            uri = user.background,
            key = remember { DateEx.TodayString },
            modifier = Modifier.fillMaxWidth().aspectRatio(2f),
            contentScale = ContentScale.Crop,
            alpha = 0.8f
        )
        UserInfoCard(
            user = user,
            isFollowed = app.config.weiboUsers.contains { it.id == user.info.id },
            onFollowClick = { onFollowClick(user, it) },
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9)
        )
    }

    @Composable
    private fun UserAlbumItem(album: WeiboAlbum, modifier: Modifier = Modifier) {
        Row(modifier = modifier) {
            WebImage(
                uri = album.pic,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f)
            )
            Column(
                modifier = Modifier.weight(1f).padding(Theme.padding.value),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Text(
                    text = album.title,
                    color = Theme.color.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = album.num)
                    SimpleEllipsisText(text = album.time, color = LocalColorVariant.current, style = Theme.typography.v8)
                }
            }
        }
    }

    @Composable
    private fun Portrait(user: WeiboUser, albums: List<WeiboAlbum>?) {
        LazyColumn(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            item(key = ItemKey("UserInfoCard")) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    UserInfoLayout(user = user)
                    Divider(modifier = Modifier.padding(bottom = Theme.padding.v))
                }
            }
            if (albums != null) {
                items(
                    items = albums,
                    key = { it.containerId }
                ) {
                    UserAlbumItem(
                        album = it,
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable { onAlbumClick(it) }
                    )
                }
            }
            item(key = ItemKey("Text")) {
                SimpleEllipsisText(
                    text = "最新微博",
                    textAlign = TextAlign.Center,
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.fillMaxWidth().padding(top = Theme.padding.v)
                )
            }
            items(
                items = items,
                key = { it.id }
            ) { weibo ->
                WeiboCard(
                    weibo = weibo,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue),
                    downloadDialog = downloadDialog
                )
            }
        }
    }

    @Composable
    private fun Landscape(user: WeiboUser, albums: List<WeiboAlbum>?) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Column(modifier = Modifier.width(Theme.size.cell1).fillMaxHeight()) {
                UserInfoLayout(user = user)
                Divider(modifier = Modifier.padding(bottom = Theme.padding.v))
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (albums == null) CircleLoading.Content(modifier = Modifier.align(Alignment.Center))
                    else if (albums.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(
                                items = albums,
                                key = { it.containerId }
                            ) {
                                UserAlbumItem(
                                    album = it,
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable { onAlbumClick(it) }
                                )
                            }
                        }
                    }
                }
            }

            StatefulBox(
                provider = provider,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                WeiboGrid(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    items = items,
                    downloadDialog = downloadDialog
                )
            }
        }
    }

    @Composable
    override fun Content() {
        val user = currentUser
        if (user != null) {
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> Portrait(user = user, albums = albums)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(user = user, albums = albums)
            }
        }
        else {
            Box(contentAlignment = Alignment.Center) { CircleLoading.Content() }
        }
    }

    private val downloadDialog = this land DialogDownload()
}