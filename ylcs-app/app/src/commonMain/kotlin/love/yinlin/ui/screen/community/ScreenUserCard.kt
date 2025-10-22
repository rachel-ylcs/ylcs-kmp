package love.yinlin.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Paid
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
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.compose.*
import love.yinlin.data.Data
import love.yinlin.data.rachel.follows.FollowStatus
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.platform.app
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenUserCard(model: AppModel, private val args: Args) : SubScreen<ScreenUserCard.Args>(model) {
    @Stable
    @Serializable
    data class Args(val uid: Int)

    private var profile: UserPublicProfile? by mutableRefStateOf(null)

    private val listState = LazyStaggeredGridState()

    private val page = object : PaginationArgs<Topic, Int, Int, Boolean>(Int.MAX_VALUE, true) {
        override fun distinctValue(item: Topic): Int = item.tid
        override fun offset(item: Topic): Int = item.tid
        override fun arg1(item: Topic): Boolean = item.isTop
    }

    private suspend fun requestUserProfile() {
        val result = ClientAPI.request(
            route = API.User.Profile.GetPublicProfile,
            data = API.User.Profile.GetPublicProfile.Request(
                token = app.config.userToken.ifEmpty { null },
                uid = args.uid
            )
        )
        if (result is Data.Success) profile = result.data
    }

    private suspend fun requestNewTopics() {
        val result = ClientAPI.request(
            route = API.User.Topic.GetTopics,
            data = API.User.Topic.GetTopics.Request(
                uid = args.uid,
                num = page.pageNum
            )
        )
        if (result is Data.Success) page.newData(result.data)
    }

    private suspend fun requestMoreTopics() {
        val result = ClientAPI.request(
            route = API.User.Topic.GetTopics,
            data = API.User.Topic.GetTopics.Request(
                uid = args.uid,
                isTop = page.arg1,
                tid = page.offset,
                num = page.pageNum
            )
        )
        if (result is Data.Success) page.moreData(result.data)
    }

    private suspend fun followUser(profile: UserPublicProfile) {
        val result = ClientAPI.request(
            route = API.User.Follows.FollowUser,
            data = API.User.Follows.FollowUser.Request(
                token = app.config.userToken,
                uid = profile.uid
            )
        )
        when (result) {
            is Data.Success -> this.profile = profile.copy(
                status = FollowStatus.FOLLOW,
                followers = profile.followers + 1
            )
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    private suspend fun unfollowUser(profile: UserPublicProfile) {
        val result = ClientAPI.request(
            route = API.User.Follows.UnfollowUser,
            data = API.User.Follows.UnfollowUser.Request(
                token = app.config.userToken,
                uid = profile.uid
            )
        )
        when (result) {
            is Data.Success -> this.profile = profile.copy(
                status = FollowStatus.UNFOLLOW,
                followers = profile.followers - 1
            )
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    private suspend fun blockUser(profile: UserPublicProfile) {
        val result = ClientAPI.request(
            route = API.User.Follows.BlockUser,
            data = API.User.Follows.BlockUser.Request(
                token = app.config.userToken,
                uid = profile.uid
            )
        )
        when (result) {
            is Data.Success -> this.profile = profile.copy(status = FollowStatus.BLOCK)
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    private fun onTopicClick(topic: Topic) {
        navigate(ScreenTopic.Args(topic))
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
            Column(modifier = Modifier.fillMaxWidth()
                .heightIn(min = cardWidth * 0.777777f)
                .clickable { onTopicClick(topic) }
            ) {
                if (topic.pic != null) {
                    WebImage(
                        uri = topic.picPath,
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
            bindPauseLoadWhenScrolling(listState)

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
            ) {  topic ->
                TopicCard(
                    topic = topic,
                    cardWidth = CustomTheme.size.cellWidth,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.equalSpace / 2)
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
                modifier = Modifier.width(CustomTheme.size.panelWidth).padding(CustomTheme.padding.equalExtraValue)
            )
            if (profile.status.canShowTopics) {
                bindPauseLoadWhenScrolling(listState)

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
                ) {  topic ->
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
        requestUserProfile()
        requestNewTopics()
    }

    override val title: String = "主页"

    @Composable
    override fun SubContent(device: Device) {
        profile?.let {
            when (device.type) {
                Device.Type.PORTRAIT -> Portrait(it)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(it)
            }
        } ?: EmptyBox()
    }
}