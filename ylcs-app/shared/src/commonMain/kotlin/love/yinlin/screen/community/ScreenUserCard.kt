package love.yinlin.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.data.rachel.follows.FollowStatus
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.cs.*

@Stable
class ScreenUserCard(manager: ScreenManager, private val uid: Int) : Screen(manager) {
    private var profile: UserPublicProfile? by mutableRefStateOf(null)

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
            this.profile = profile.copy(status = FollowStatus.FOLLOW, followers = profile.followers + 1)
        }.errorTip
    }

    private suspend fun unfollowUser(profile: UserPublicProfile) {
        ApiFollowsUnfollowUser.request(app.config.userToken, profile.uid) {
            this.profile = profile.copy(status = FollowStatus.UNFOLLOW, followers = profile.followers - 1)
        }.errorTip
    }

    private suspend fun blockUser(profile: UserPublicProfile) {
        ApiFollowsBlockUser.request(app.config.userToken, profile.uid) {
            this.profile = profile.copy(status = FollowStatus.BLOCK)
        }.errorTip
    }

    private fun onTopicClick(topic: Topic) {
        navigate(::ScreenTopic, topic)
    }

    @Composable
    private fun TopicCard(
        topic: Topic,
        cardWidth: Dp,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = cardWidth * 0.777777f)
                    .clickable { onTopicClick(topic) }
            ) {
                topic.picPath?.url?.let {
                    WebImage(
                        uri = it,
                        modifier = Modifier.fillMaxWidth().height(cardWidth * 1.333333f),
                        contentScale = ContentScale.Crop
                    )
                }
                if (topic.isTop) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BoxText(text = "置顶", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    text = topic.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value)
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NormalText(
                        text = topic.commentNum.toString(),
                        icon = Icons.AutoMirrored.Outlined.Comment,
                        style = MaterialTheme.typography.bodySmall,
                        padding = CustomTheme.padding.zeroValue,
                        modifier = Modifier.weight(1f)
                    )
                    NormalText(
                        text = topic.coinNum.toString(),
                        icon = Icons.Outlined.Paid,
                        style = MaterialTheme.typography.bodySmall,
                        padding = CustomTheme.padding.zeroValue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun UserProfileCardLayout(
        profile: UserPublicProfile,
        shape: Shape = RectangleShape,
        modifier: Modifier = Modifier
    ) {
        UserPublicProfileCard(
            profile = profile,
            shape = shape,
            modifier = modifier
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val status = profile.status
                when (val isFollowed = status.isFollowed) {
                    null -> {}
                    else -> {
                        LoadingIcon(
                            icon = if (isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            tip = if (isFollowed) "取消关注" else "关注",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { if (isFollowed) unfollowUser(profile) else followUser(profile) }
                        )
                    }
                }
                if (status.canBlock) {
                    LoadingIcon(
                        icon = Icons.Filled.Block,
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            if (slot.confirm.openSuspend(content = "拉黑对方")) blockUser(profile)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun Portrait(profile: UserPublicProfile) {
        if (profile.status.canShowTopics) {
            PauseLoading(listState)

            PaginationStaggeredGrid(
                items = page.items,
                key = { it.tid },
                columns = StaggeredGridCells.Adaptive(CustomTheme.size.cellWidth),
                state = listState,
                canRefresh = false,
                canLoading = page.canLoading,
                onLoading = { requestMoreTopics() },
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
                verticalItemSpacing = CustomTheme.padding.equalSpace,
                header = {
                    UserProfileCardLayout(
                        profile = profile,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) { topic ->
                TopicCard(
                    topic = topic,
                    cardWidth = CustomTheme.size.cellWidth,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = CustomTheme.padding.equalSpace / 2)
                )
            }
        }
        else {
            UserProfileCardLayout(
                profile = profile,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun Landscape(profile: UserPublicProfile) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            UserProfileCardLayout(
                profile = profile,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.width(CustomTheme.size.panelWidth)
                    .padding(CustomTheme.padding.equalExtraValue)
            )
            if (profile.status.canShowTopics) {
                PauseLoading(listState)

                PaginationStaggeredGrid(
                    items = page.items,
                    key = { it.tid },
                    columns = StaggeredGridCells.Adaptive(CustomTheme.size.cellWidth),
                    state = listState,
                    canRefresh = false,
                    canLoading = page.canLoading,
                    onLoading = { requestMoreTopics() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = CustomTheme.padding.equalValue,
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                    verticalItemSpacing = CustomTheme.padding.equalSpace
                ) { topic ->
                    TopicCard(
                        topic = topic,
                        cardWidth = CustomTheme.size.cellWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    override suspend fun initialize() {
        launch { ApiProfileGetPublicProfile.request(app.config.userToken.ifEmpty { null }, uid) { profile = it } }
        launch { ApiTopicGetTopics.request(uid, page.default1, page.default, page.pageNum) { page.newData(it) } }
    }

    override val title: String = "主页"

    @Composable
    override fun Content(device: Device) {
        profile?.let {
            when (device.type) {
                Device.Type.PORTRAIT -> Portrait(it)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(it)
            }
        } ?: EmptyBox()
    }
}