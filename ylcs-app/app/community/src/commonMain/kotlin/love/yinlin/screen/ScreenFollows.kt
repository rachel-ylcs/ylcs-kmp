package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastMap
import love.yinlin.app
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationGrid
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.*
import love.yinlin.data.rachel.follows.BlockedUserInfo
import love.yinlin.data.rachel.follows.FollowInfo
import love.yinlin.data.rachel.follows.FollowTabItem
import love.yinlin.data.rachel.follows.FollowerInfo
import love.yinlin.extension.DateEx

@Stable
class ScreenFollows(initTabItem: FollowTabItem) : Screen() {
    @Stable
    private data class FollowItem(val fid: Long, val uid: Int, val name: String) {
        val avatarPath: String by lazy { ServerRes.Users.User(uid).avatar.url }
    }

    private var tab by mutableRefStateOf(initTabItem)
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
            FollowTabItem.Follows -> pageFollows.items.fastMap { FollowItem(it.fid, it.uid, it.name) }
            FollowTabItem.Followers -> pageFollowers.items.fastMap { FollowItem(it.fid, it.uid, it.name) }
            FollowTabItem.BlockUsers -> pageBlockUsers.items.fastMap { FollowItem(it.fid, it.uid, it.name) }
        }
    }

    private val page get() = when (tab) {
        FollowTabItem.Follows -> pageFollows
        FollowTabItem.Followers -> pageFollowers
        FollowTabItem.BlockUsers -> pageBlockUsers
    }

    private suspend fun requestNewData() {
        when (tab) {
            FollowTabItem.Follows -> ApiFollowsGetFollows.request(app.config.userToken, pageFollows.default, pageFollows.default1, pageFollows.pageNum) {
                pageFollows.newData(it)
            }.errorTip
            FollowTabItem.Followers -> ApiFollowsGetFollowers.request(app.config.userToken, pageFollowers.default, pageFollowers.default1, pageFollowers.pageNum) {
                pageFollowers.newData(it)
            }.errorTip
            FollowTabItem.BlockUsers -> ApiFollowsGetBlockedUsers.request(app.config.userToken, pageBlockUsers.default, pageBlockUsers.pageNum) {
                pageBlockUsers.newData(it)
            }.errorTip
        }
        gridState.requestScrollToItem(0)
    }

    private suspend fun requestMoreData() {
        when (tab) {
            FollowTabItem.Follows -> ApiFollowsGetFollows.request(app.config.userToken, pageFollows.offset, pageFollows.arg1, pageFollows.pageNum) {
                pageFollows.moreData(it)
            }
            FollowTabItem.Followers -> ApiFollowsGetFollowers.request(app.config.userToken, pageFollowers.offset, pageFollowers.arg1, pageFollowers.pageNum) {
                pageFollowers.moreData(it)
            }
            FollowTabItem.BlockUsers -> ApiFollowsGetBlockedUsers.request(app.config.userToken, pageBlockUsers.offset, pageBlockUsers.pageNum) {
                pageBlockUsers.moreData(it)
            }
        }
    }

    private suspend fun unBlockUser(item: FollowItem) {
        ApiFollowsUnblockUser.request(app.config.userToken, item.uid) {
            pageBlockUsers.items.removeAll { it.fid == item.fid }
        }.errorTip
    }

    override val title: String get() = tab.title

    override suspend fun initialize() {
        requestNewData()
    }

    @Composable
    private fun FollowItemLayout(
        item: FollowItem,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Row(
            modifier = modifier.clickable(onClick = onClick).padding(Theme.padding.value),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            WebImage(
                uri = item.avatarPath,
                key = remember { DateEx.TodayString },
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.size(Theme.size.image9)
            )
            SimpleEllipsisText(text = item.name, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Refresh, tip = "刷新", onClick = ::requestNewData)
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = Theme.shadow.v3
            ) {
                TabBar(
                    size = FollowTabItem.entries.size,
                    index = tab.ordinal,
                    onNavigate = {
                        tab = FollowTabItem.entries[it]
                        launch {
                            if (items.isEmpty()) requestNewData()
                        }
                    },
                    titleProvider = { FollowTabItem.entries[it].title },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (items.isNotEmpty()) {
                    PaginationGrid(
                        items = items,
                        key = { it.fid },
                        columns = GridCells.Adaptive(Theme.size.cell2),
                        state = gridState,
                        canRefresh = true,
                        canLoading = page.canLoading,
                        onRefresh = ::requestNewData,
                        onLoading = ::requestMoreData,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FollowItemLayout(
                            item = it,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (tab != FollowTabItem.BlockUsers) navigate(::ScreenUserCard, it.uid)
                                else {
                                    launch {
                                        if (slot.confirm.open(content = "取消拉黑")) unBlockUser(it)
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