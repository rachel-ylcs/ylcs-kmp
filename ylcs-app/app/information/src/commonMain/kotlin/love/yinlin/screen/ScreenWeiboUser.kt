package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import love.yinlin.app
import love.yinlin.common.BasicWeiboManager
import love.yinlin.common.MessageType
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.itemKey
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.extension.DateEx
import love.yinlin.extension.then
import love.yinlin.tpl.weibo.WeiboAPI

@Stable
class ScreenWeiboUser(private val userId: String) : Screen() {
    private val provider = RachelStatefulProvider()
    private var items: List<Weibo> by mutableRefStateOf([])
    private val listState = LazyStaggeredGridState()
    private var currentWeiboUser: WeiboUser? by mutableRefStateOf(null)
    private var albums: List<WeiboAlbum>? by mutableRefStateOf(null)

    init {
        land(BasicWeiboManager.CommonDownloadDialog)
    }

    private fun onFollowClick(weiboUser: WeiboUser, isFollow: Boolean) {
        val weiboUsers = app.config.weiboUsers
        if (isFollow) {
            if (!weiboUsers.contains { it.id == weiboUser.user.id }) weiboUsers += weiboUser.user
        }
        else weiboUsers.removeAll { it.id == weiboUser.user.id }
    }

    private fun onAlbumClick(album: WeiboAlbum) {
        // navigate(::ScreenWeiboAlbum, album.containerId, album.title)
    }

    override val title: String get() = currentWeiboUser?.user?.name ?: ""

    override suspend fun initialize() {
        supervisorScope {
            val cookie = WeiboAPI.generateCookie()

            // 请求微博
            this.launch {
                currentWeiboUser = WeiboAPI.requestUser(userId, cookie)
                currentWeiboUser?.user?.id?.then { id ->
                    provider.withLoading {
                        val newItems = mutableMapOf<String, Weibo>()
                        newItems += WeiboAPI.requestUserWeibo(id, cookie)!!.associateBy { it.id }
                        items = newItems.map { it.value }.sortedDescending()
                        newItems.isNotEmpty()
                    }
                }
            }

            // 请求相册
            this.launch {
                albums = WeiboAPI.requestUserAlbum(userId, cookie)
            }
        }
    }

    @Composable
    private fun UserInfoCard(
        weiboUser: WeiboUser,
        isFollowed: Boolean,
        onFollowClick: (Boolean) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
        ) {
            WebImage(
                uri = weiboUser.user.avatar,
                key = DateEx.TodayLong,
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
                        text = weiboUser.user.name,
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
                    SimpleEllipsisText(text = "关注 ${weiboUser.followNum}")
                    SimpleEllipsisText(text = "粉丝 ${weiboUser.fansNum}")
                }
            }
        }
    }

    private val userInfoLayout = movableComposable { weiboUser: WeiboUser ->
        WebImage(
            uri = weiboUser.background,
            key = DateEx.TodayLong,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f),
            contentScale = ContentScale.Crop,
            alpha = 0.75f
        )
        UserInfoCard(
            weiboUser = weiboUser,
            isFollowed = app.config.weiboUsers.contains { it.id == weiboUser.user.id },
            onFollowClick = { onFollowClick(weiboUser, it) },
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9)
        )
        HorizontalDivider(modifier = Modifier.padding(bottom = Theme.padding.v))
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
    private fun Portrait(manager: BasicWeiboManager, weiboUser: WeiboUser, albums: List<WeiboAlbum>?) {
        LazyColumn(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            itemKey("UserInfoCard") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    userInfoLayout(weiboUser)
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
            itemKey("Text") {
                SimpleEllipsisText(
                    text = "最新微博",
                    textAlign = TextAlign.Center,
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.fillMaxWidth().padding(top = Theme.padding.v)
                )
            }
            items(items = items, key = { it.id }) { weibo ->
                with(manager) {
                    MessageLayout(
                        modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue),
                        message = weibo
                    )
                }
            }
        }
    }

    @Composable
    private fun Landscape(manager: BasicWeiboManager, weiboUser: WeiboUser, albums: List<WeiboAlbum>?) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Column(modifier = Modifier.width(Theme.size.cell1).fillMaxHeight()) {
                userInfoLayout(weiboUser)
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (albums == null) CircleLoading.Content(modifier = Modifier.align(Alignment.Center))
                    else if (albums.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(items = albums, key = { it.containerId }) {
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
//                WeiboGrid(
//                    state = listState,
//                    modifier = Modifier.fillMaxSize(),
//                    items = items,
//                    downloadDialog = downloadDialog
//                )
            }
        }
    }

    @Composable
    override fun Content() {
        val weiboUser = currentWeiboUser
        val manager = DataSourceInformation.managers[MessageType.Weibo]!!
        if (weiboUser != null) {
            val deviceType by rememberDeviceType()
            when (deviceType) {
                Device.Type.PORTRAIT -> Portrait(manager = manager, weiboUser = weiboUser, albums = albums)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(manager = manager, weiboUser = weiboUser, albums = albums)
            }
        }
        else {
            Box(contentAlignment = Alignment.Center) { CircleLoading.Content() }
        }
    }
}