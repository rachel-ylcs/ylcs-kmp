package love.yinlin.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.data.Data
import love.yinlin.data.rachel.follows.BlockedUserInfo
import love.yinlin.data.rachel.follows.FollowInfo
import love.yinlin.data.rachel.follows.FollowerInfo
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.container.TabBar
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.service
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationGrid

@Stable
enum class FollowTabItem(val title: String) {
    FOLLOWS("关注"), FOLLOWERS("粉丝"), BLOCK_USERS("黑名单");

    companion object {
        fun fromInt(value: Int): FollowTabItem = when (value) {
            FOLLOWERS.ordinal -> FOLLOWERS
            BLOCK_USERS.ordinal -> BLOCK_USERS
            else -> FOLLOWS
        }
    }
}

@Stable
@Serializable
private data class FollowItem(val fid: Long, val uid: Int, val name: String) {
    val avatarPath: String by lazy { "${Local.API_BASE_URL}/${ServerRes.Users.User(uid).avatar}" }
}

@Composable
private fun FollowItemLayout(
    item: FollowItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick).padding(CustomTheme.padding.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
    ) {
        WebImage(
            uri = item.avatarPath,
            key = remember { DateEx.TodayString },
            contentScale = ContentScale.Crop,
            circle = true,
            modifier = Modifier.size(CustomTheme.size.microImage)
        )
        Text(
            text = item.name,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Stable
class ScreenFollows(manager: ScreenManager, args: Args) : Screen<ScreenFollows.Args>(manager) {
    @Stable
    @Serializable
    data class Args(val tab: Int)

    private var tab by mutableRefStateOf(FollowTabItem.fromInt(args.tab))
    private val gridState = LazyGridState()

    private val pageFollows = object : PaginationArgs<FollowInfo, Long, Int, Long>(Int.MAX_VALUE, 0L) {
        override fun distinctValue(item: FollowInfo): Long = item.fid
        override fun offset(item: FollowInfo): Int = item.score
        override fun arg1(item: FollowInfo): Long = item.fid
    }

    private val pageFollowers = object : PaginationArgs<FollowerInfo, Long, Int, Long>(Int.MAX_VALUE, 0L) {
        override fun distinctValue(item: FollowerInfo): Long = item.fid
        override fun offset(item: FollowerInfo): Int = item.score
        override fun arg1(item: FollowerInfo): Long = item.fid
    }

    private val pageBlockUsers = object : Pagination<BlockedUserInfo, Long, Long>(0L) {
        override fun distinctValue(item: BlockedUserInfo): Long = item.fid
        override fun offset(item: BlockedUserInfo): Long = item.fid
    }

    private val items by derivedStateOf {
        when (tab) {
            FollowTabItem.FOLLOWS -> pageFollows.items.fastMap { FollowItem(it.fid, it.uid, it.name) }
            FollowTabItem.FOLLOWERS -> pageFollowers.items.fastMap { FollowItem(it.fid, it.uid, it.name) }
            FollowTabItem.BLOCK_USERS -> pageBlockUsers.items.fastMap { FollowItem(it.fid, it.uid, it.name) }
        }
    }

    private val page by derivedStateOf {
        when (tab) {
            FollowTabItem.FOLLOWS -> pageFollows
            FollowTabItem.FOLLOWERS -> pageFollowers
            FollowTabItem.BLOCK_USERS -> pageBlockUsers
        }
    }

    private suspend fun requestNewData() {
        when (tab) {
            FollowTabItem.FOLLOWS -> {
                val result = ClientAPI.request(
                    route = API.User.Follows.GetFollows,
                    data = API.User.Follows.GetFollows.Request(
                        token = service.config.userToken,
                        num = pageFollows.pageNum
                    )
                )
                when (result) {
                    is Data.Success -> {
                        pageFollows.newData(result.data)
                        gridState.scrollToItem(0)
                    }
                    is Data.Failure -> slot.tip.error(result.message)
                }
            }
            FollowTabItem.FOLLOWERS -> {
                val result = ClientAPI.request(
                    route = API.User.Follows.GetFollowers,
                    data = API.User.Follows.GetFollowers.Request(
                        token = service.config.userToken,
                        num = pageFollowers.pageNum
                    )
                )
                when (result) {
                    is Data.Success -> {
                        pageFollowers.newData(result.data)
                        gridState.scrollToItem(0)
                    }
                    is Data.Failure -> slot.tip.error(result.message)
                }
            }
            FollowTabItem.BLOCK_USERS -> {
                val result = ClientAPI.request(
                    route = API.User.Follows.GetBlockedUsers,
                    data = API.User.Follows.GetBlockedUsers.Request(
                        token = service.config.userToken,
                        num = pageBlockUsers.pageNum
                    )
                )
                when (result) {
                    is Data.Success -> {
                        pageBlockUsers.newData(result.data)
                        gridState.scrollToItem(0)
                    }
                    is Data.Failure -> slot.tip.error(result.message)
                }
            }
        }
    }

    private suspend fun requestMoreData() {
        when (tab) {
            FollowTabItem.FOLLOWS -> {
                val result = ClientAPI.request(
                    route = API.User.Follows.GetFollows,
                    data = API.User.Follows.GetFollows.Request(
                        token = service.config.userToken,
                        score = pageFollows.offset,
                        fid = pageFollows.arg1,
                        num = pageFollows.pageNum
                    )
                )
                if (result is Data.Success) pageFollows.moreData(result.data)
            }
            FollowTabItem.FOLLOWERS -> {
                val result = ClientAPI.request(
                    route = API.User.Follows.GetFollowers,
                    data = API.User.Follows.GetFollowers.Request(
                        token = service.config.userToken,
                        score = pageFollowers.offset,
                        fid = pageFollowers.arg1,
                        num = pageFollowers.pageNum
                    )
                )
                if (result is Data.Success) pageFollowers.moreData(result.data)
            }
            FollowTabItem.BLOCK_USERS -> {
                val result = ClientAPI.request(
                    route = API.User.Follows.GetBlockedUsers,
                    data = API.User.Follows.GetBlockedUsers.Request(
                        token = service.config.userToken,
                        fid = pageBlockUsers.offset,
                        num = pageBlockUsers.pageNum
                    )
                )
                if (result is Data.Success) pageBlockUsers.moreData(result.data)
            }
        }
    }

    private suspend fun unBlockUser(item: FollowItem) {
        val result = ClientAPI.request(
            route = API.User.Follows.UnblockUser,
            data = API.User.Follows.UnblockUser.Request(
                token = service.config.userToken,
                uid = item.uid
            )
        )
        when (result) {
            is Data.Success -> pageBlockUsers.items.removeAll { it.fid == item.fid }
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    override val title: String? by derivedStateOf { tab.title }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(Icons.Outlined.Refresh, "刷新") { requestNewData() }
    }

    override suspend fun initialize() {
        requestNewData()
    }

    @Composable
    override fun Content(device: Device) {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = CustomTheme.shadow.surface
            ) {
                TabBar(
                    currentPage = tab.ordinal,
                    onNavigate = {
                        tab = FollowTabItem.fromInt(it)
                        launch { if (items.isEmpty()) requestNewData() }
                    },
                    items = remember { FollowTabItem.entries.fastMap { it.title } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (items.isEmpty()) EmptyBox()
                else {
                    PauseLoading(gridState)

                    PaginationGrid(
                        items = items,
                        key = { it.fid },
                        columns = GridCells.Adaptive(CustomTheme.size.cardWidth),
                        state = gridState,
                        canRefresh = true,
                        canLoading = page.canLoading,
                        onRefresh = { requestNewData() },
                        onLoading = { requestMoreData() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FollowItemLayout(
                            item = it,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (tab != FollowTabItem.BLOCK_USERS) navigate(ScreenUserCard.Args(it.uid))
                                else {
                                    launch {
                                        if (slot.confirm.openSuspend(content = "取消拉黑")) unBlockUser(it)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}