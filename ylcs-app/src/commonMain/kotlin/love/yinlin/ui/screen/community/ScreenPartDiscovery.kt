package love.yinlin.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.container.TabBar
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.PaginationArgs

@Stable
@Serializable
private enum class DiscoveryItem(
    val id: Int,
    val icon: ImageVector
) {
    Latest(Comment.Section.LATEST, Icons.Filled.NewReleases),
    Hot(Comment.Section.HOT, Icons.Filled.LocalFireDepartment),
    Notification(Comment.Section.NOTIFICATION, Icons.Filled.Campaign),
    Water(Comment.Section.WATER, Icons.Filled.WaterDrop),
    Activity(Comment.Section.ACTIVITY, Icons.Filled.Celebration),
    Discussion(Comment.Section.DISCUSSION, Icons.AutoMirrored.Filled.Chat);

    companion object {
        @Stable
        val items = DiscoveryItem.entries.map { Comment.Section.sectionName(it.id) to it.icon }
    }
}

@Stable
class ScreenPartDiscovery(model: AppModel) : ScreenPart(model) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private val listState = LazyStaggeredGridState()

    private var currentPage by mutableIntStateOf(0)
    val currentSection: Int get() = DiscoveryItem.entries[currentPage].id

    val page = object : PaginationArgs<Topic, Int, Double>(Int.MAX_VALUE, Double.MAX_VALUE) {
        override fun offset(item: Topic): Int = item.tid
        override fun arg1(item: Topic): Double = item.score
    }

    private suspend fun requestNewData() {
        state = BoxState.LOADING
        val section = currentSection
        val result = when (section) {
            DiscoveryItem.Latest.id -> ClientAPI.request(
                route = API.User.Topic.GetLatestTopics,
                data = API.User.Topic.GetLatestTopics.Request(
                    num = page.pageNum
                )
            )
            DiscoveryItem.Hot.id -> ClientAPI.request(
                route = API.User.Topic.GetHotTopics,
                data = API.User.Topic.GetHotTopics.Request(
                    num = page.pageNum
                )
            )
            else -> ClientAPI.request(
                route = API.User.Topic.GetSectionTopics,
                data = API.User.Topic.GetSectionTopics.Request(
                    section = section,
                    num = page.pageNum
                )
            )
        }
        if (result is Data.Success) {
            state = if (page.newData(result.data)) BoxState.CONTENT else BoxState.EMPTY
            listState.scrollToItem(0)
        }
        else state = BoxState.NETWORK_ERROR
    }

    private suspend fun requestMoreData() {
        val section = currentSection
        val result = when (section) {
            DiscoveryItem.Latest.id -> ClientAPI.request(
                route = API.User.Topic.GetLatestTopics,
                data = API.User.Topic.GetLatestTopics.Request(
                    tid = page.offset,
                    num = page.pageNum
                )
            )
            DiscoveryItem.Hot.id -> ClientAPI.request(
                route = API.User.Topic.GetHotTopics,
                data = API.User.Topic.GetHotTopics.Request(
                    score = page.arg1,
                    tid = page.offset,
                    num = page.pageNum
                )
            )
            else -> ClientAPI.request(
                route = API.User.Topic.GetSectionTopics,
                data = API.User.Topic.GetSectionTopics.Request(
                    section = section,
                    tid = page.offset,
                    num = page.pageNum
                )
            )
        }
        if (result is Data.Success) page.moreData(result.data)
    }

    private fun onTopicClick(topic: Topic) {
        navigate(ScreenTopic.Args(topic))
    }

    fun onUserAvatarClick(uid: Int) {
        navigate(ScreenUserCard.Args(uid))
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
            shadowElevation = ThemeValue.Shadow.Surface
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
                Text(
                    text = topic.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(ThemeValue.Padding.Value),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
                        WebImage(
                            uri = topic.avatarPath,
                            key = DateEx.TodayString,
                            contentScale = ContentScale.Crop,
                            circle = true,
                            modifier = Modifier.matchParentSize(),
                            onClick = { onUserAvatarClick(topic.uid) }
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                    ) {
                        Text(
                            text = topic.name,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RachelText(
                                text = topic.commentNum.toString(),
                                icon = Icons.AutoMirrored.Outlined.Comment,
                                style = MaterialTheme.typography.bodySmall,
                                padding = ThemeValue.Padding.ZeroValue,
                                modifier = Modifier.weight(1f)
                            )
                            RachelText(
                                text = topic.coinNum.toString(),
                                icon = Icons.Outlined.Paid,
                                style = MaterialTheme.typography.bodySmall,
                                padding = ThemeValue.Padding.ZeroValue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun initialize() {
        requestNewData()
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = ThemeValue.Shadow.Surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(immersivePadding.withoutBottom),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabBar(
                        currentPage = currentPage,
                        onNavigate = {
                            currentPage = it
                            launch { requestNewData() }
                        },
                        items = DiscoveryItem.items,
                        modifier = Modifier.weight(1f).padding(end = ThemeValue.Padding.HorizontalSpace)
                    )
                    ActionScope.Right.Actions {
                        Action(Icons.Outlined.Add) {
                            navigate<ScreenAddTopic>()
                        }
                    }
                }
            }

            StatefulBox(
                state = state,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(immersivePadding.withoutTop)
            ) {
                PaginationStaggeredGrid(
                    items = page.items,
                    key = { it.tid },
                    columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CellWidth),
                    state = listState,
                    canRefresh = true,
                    canLoading = page.canLoading,
                    onRefresh = { requestNewData() },
                    onLoading = { requestMoreData() },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = ThemeValue.Padding.EqualValue,
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                    verticalItemSpacing = ThemeValue.Padding.EqualSpace
                ) { topic ->
                    TopicCard(
                        topic = topic,
                        cardWidth = ThemeValue.Size.CellWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}