package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.BoxText
import love.yinlin.compose.ui.common.PortraitValue
import love.yinlin.compose.ui.common.UserProfileInfoColumn
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.LoadingTextButton
import love.yinlin.compose.ui.input.PrimaryLoadingTextButton
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.cs.*
import love.yinlin.data.rachel.follows.FollowStatus
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.data.rachel.topic.Topic

@Stable
class ScreenUserCard(private val uid: Int) : Screen() {
    private var currentProfile: UserPublicProfile? by mutableRefStateOf(null)

    private val listState = LazyStaggeredGridState()

    private val page = object : PaginationArgs<Topic, Int, Int, Boolean>(
        default = Int.MAX_VALUE,
        default1 = true,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: Topic): Int = item.tid
        override fun offset(item: Topic): Int = item.tid
        override fun arg1(item: Topic): Boolean = item.isTop
    }

    private suspend fun requestMoreTopics() {
        ApiTopicGetTopics.request(uid, page.arg1, page.offset, page.pageNum) { page.moreData(it) }
    }

    private suspend fun followUser(profile: UserPublicProfile) {
        ApiFollowsFollowUser.request(app.config.userToken, profile.uid) {
            currentProfile = profile.copy(status = FollowStatus.FOLLOW, followers = profile.followers + 1)
        }.errorTip
    }

    private suspend fun unfollowUser(profile: UserPublicProfile) {
        ApiFollowsUnfollowUser.request(app.config.userToken, profile.uid) {
            currentProfile = profile.copy(status = FollowStatus.UNFOLLOW, followers = profile.followers - 1)
        }.errorTip
    }

    private suspend fun blockUser(profile: UserPublicProfile) {
        ApiFollowsBlockUser.request(app.config.userToken, profile.uid) {
            currentProfile = profile.copy(status = FollowStatus.BLOCK)
        }.errorTip
    }

    override val title: String = "主页"

    override suspend fun initialize() {
        supervisorScope {
            this.launch {
                ApiProfileGetPublicProfile.request(app.config.userToken.ifEmpty { null }, uid) { currentProfile = it }
            }
            this.launch {
                ApiTopicGetTopics.request(uid, page.default1, page.default, page.pageNum) { page.newData(it) }
            }
        }
    }

    @Composable
    private fun TopicCard(topic: Topic, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v3,
            onClick = { navigate(::ScreenTopic, topic) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = Theme.size.cell4 * 0.777777f)) {
                topic.picPath?.url?.let {
                    WebImage(
                        uri = it,
                        modifier = Modifier.fillMaxWidth().height(Theme.size.cell4 * 1.333333f),
                        contentScale = ContentScale.Crop
                    )
                }
                if (topic.isTop) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BoxText(text = "置顶", color = Theme.color.primary)
                    }
                }
                Text(
                    text = topic.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)
                )
                Box(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextIconAdapter(modifier = Modifier.weight(1f)) { idIcon, idText ->
                        Icon(icon = Icons.Comment, modifier = Modifier.idIcon())
                        SimpleClipText(
                            text = topic.commentNum.toString(),
                            style = Theme.typography.v8,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.idText()
                        )
                    }
                    TextIconAdapter(modifier = Modifier.weight(1f)) { idIcon, idText ->
                        Icon(icon = Icons.Paid, modifier = Modifier.idIcon())
                        SimpleClipText(
                            text = topic.coinNum.toString(),
                            style = Theme.typography.v8,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.idText()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun UserProfileCard(profile: UserPublicProfile, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            contentAlignment = Alignment.TopCenter,
            shadowElevation = Theme.shadow.v3
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                UserProfileInfoColumn(profile = profile, onLevelClick = null)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PortraitValue(value = profile.level.toString(), title = "等级")
                    PortraitValue(value = profile.follows.toString(), title = "关注")
                    PortraitValue(value = profile.followers.toString(), title = "粉丝")
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val status = profile.status
                    when (val isFollowed = status.isFollowed) {
                        null -> {}
                        else -> {
                            PrimaryLoadingTextButton(
                                text = if (isFollowed) "取消关注" else "关注",
                                icon = if (isFollowed) Icons.Favorite else Icons.FavoriteBorder,
                                onClick = { if (isFollowed) unfollowUser(profile) else followUser(profile) }
                            )
                        }
                    }
                    if (status.canBlock) {
                        LoadingTextButton(
                            text = "拉黑",
                            icon = Icons.Block,
                            color = Theme.color.error,
                            onClick = {
                                if (slot.confirm.open(content = "拉黑对方")) blockUser(profile)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait(profile: UserPublicProfile) {
        if (profile.status.canShowTopics) {
            PaginationStaggeredGrid(
                items = page.items,
                key = { it.tid },
                columns = StaggeredGridCells.Adaptive(Theme.size.cell4),
                state = listState,
                canRefresh = false,
                canLoading = page.canLoading,
                onLoading = ::requestMoreTopics,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
                verticalItemSpacing = Theme.padding.e,
                header = { UserProfileCard(profile = profile, modifier = Modifier.fillMaxWidth()) }
            ) { topic ->
                TopicCard(topic = topic, modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.e / 2))
            }
        }
        else UserProfileCard(profile = profile, modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxWidth())
    }

    @Composable
    private fun Landscape(profile: UserPublicProfile) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            UserProfileCard(profile = profile, modifier = Modifier.width(Theme.size.cell1).fillMaxHeight())

            if (profile.status.canShowTopics) {
                PaginationStaggeredGrid(
                    items = page.items,
                    key = { it.tid },
                    columns = StaggeredGridCells.Adaptive(Theme.size.cell4),
                    state = listState,
                    canRefresh = false,
                    canLoading = page.canLoading,
                    onLoading = ::requestMoreTopics,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = Theme.padding.eValue,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                    verticalItemSpacing = Theme.padding.e
                ) { topic ->
                    TopicCard(topic = topic, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    override fun Content() {
        val profile = currentProfile
        if (profile != null) {
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> Portrait(profile)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(profile)
            }
        }
    }
}