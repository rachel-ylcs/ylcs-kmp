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
import love.yinlin.api.*
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.data.rachel.follows.BlockedUserInfo
import love.yinlin.data.rachel.follows.FollowInfo
import love.yinlin.data.rachel.follows.FollowerInfo
import love.yinlin.extension.DateEx
import love.yinlin.compose.ui.container.TabBar
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationGrid

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
    val avatarPath: String by lazy { ServerRes.Users.User(uid).avatar.url }
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
class ScreenFollows(manager: ScreenManager, currentTab: Int) : Screen(manager) {
    private var tab by mutableRefStateOf(FollowTabItem.fromInt(currentTab))
    private val gridState = LazyGridState()

    private val pageFollows = object : PaginationArgs<FollowInfo, Long, Int, Long>(
        default = Int.MAX_VALUE,
        default1 = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: FollowInfo): Long = item.fid
        override fun offset(item: FollowInfo): Int = item.score
        override fun arg1(item: FollowInfo): Long = item.fid
    }

    private val pageFollowers = object : PaginationArgs<FollowerInfo, Long, Int, Long>(
        default = Int.MAX_VALUE,
        default1 = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: FollowerInfo): Long = item.fid
        override fun offset(item: FollowerInfo): Int = item.score
        override fun arg1(item: FollowerInfo): Long = item.fid
    }

    private val pageBlockUsers = object : Pagination<BlockedUserInfo, Long, Long>(
        default = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
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
            FollowTabItem.FOLLOWS -> ApiFollowsGetFollows.request(app.config.userToken, pageFollows.default, pageFollows.default1, pageFollows.pageNum) {
                pageFollows.newData(it)
                gridState.scrollToItem(0)
            }.errorTip
            FollowTabItem.FOLLOWERS -> ApiFollowsGetFollowers.request(app.config.userToken, pageFollowers.default, pageFollowers.default1, pageFollowers.pageNum) {
                pageFollowers.newData(it)
                gridState.scrollToItem(0)
            }.errorTip
            FollowTabItem.BLOCK_USERS -> ApiFollowsGetBlockedUsers.request(app.config.userToken, pageBlockUsers.default, pageBlockUsers.pageNum) {
                pageBlockUsers.newData(it)
                gridState.scrollToItem(0)
            }.errorTip
        }
    }

    private suspend fun requestMoreData() {
        when (tab) {
            FollowTabItem.FOLLOWS -> ApiFollowsGetFollows.request(app.config.userToken, pageFollows.offset, pageFollows.arg1, pageFollows.pageNum) {
                pageFollows.moreData(it)
            }
            FollowTabItem.FOLLOWERS -> ApiFollowsGetFollowers.request(app.config.userToken, pageFollowers.offset, pageFollowers.arg1, pageFollowers.pageNum) {
                pageFollowers.moreData(it)
            }
            FollowTabItem.BLOCK_USERS -> ApiFollowsGetBlockedUsers.request(app.config.userToken, pageBlockUsers.offset, pageBlockUsers.pageNum) {
                pageBlockUsers.moreData(it)
            }
        }
    }

    private suspend fun unBlockUser(item: FollowItem) {
        ApiFollowsUnblockUser.request(app.config.userToken, item.uid) {
            pageBlockUsers.items.removeAll { it.fid == item.fid }
        }.errorTip
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
                                if (tab != FollowTabItem.BLOCK_USERS) navigate(::ScreenUserCard, it.uid)
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