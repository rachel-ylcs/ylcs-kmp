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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastMap
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.container.TabBar
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.FABAction

@Stable
@Serializable
private enum class DiscoveryItem(
    val id: Int,
    val icon: ImageVector
) {
    LatestTopic(Comment.Section.LATEST_TOPIC, Icons.Filled.NewReleases),
    LatestComment(Comment.Section.LATEST_COMMENT, Icons.Filled.NewReleases),
    Hot(Comment.Section.HOT, Icons.Filled.LocalFireDepartment),
    Notification(Comment.Section.NOTIFICATION, Icons.Filled.Campaign),
    Water(Comment.Section.WATER, Icons.Filled.WaterDrop),
    Activity(Comment.Section.ACTIVITY, Icons.Filled.Celebration),
    Discussion(Comment.Section.DISCUSSION, Icons.AutoMirrored.Filled.Chat);

    companion object {
        @Stable
        val items = DiscoveryItem.entries.fastMap { Comment.Section.sectionName(it.id) to it.icon }
    }
}

@Stable
class ScreenPartDiscovery(model: AppModel) : ScreenPart(model) {
    private var state: BoxState by mutableStateOf(EMPTY)

    private val gridState = LazyStaggeredGridState()

    private var currentPage by mutableIntStateOf(0)
    val currentSection: Int get() = DiscoveryItem.entries[currentPage].id

    val page = object : PaginationArgs<Topic, Int, Int, Double>(Int.MAX_VALUE, Double.MAX_VALUE) {
        override fun distinctValue(item: Topic): Int = item.tid
        override fun offset(item: Topic): Int = item.tid
        override fun arg1(item: Topic): Double = item.score
    }

    private suspend fun requestNewData(loading: Boolean) {
        if (state != LOADING) {
            if (loading) state = LOADING
            val result = when (val section = currentSection) {
                DiscoveryItem.LatestTopic.id -> ClientAPI.request(
                    route = API.User.Topic.GetLatestTopics,
                    data = API.User.Topic.GetLatestTopics.Request(
                        num = page.pageNum
                    )
                )
                DiscoveryItem.LatestComment.id -> ClientAPI.request(
                    route = API.User.Topic.GetLatestTopicsByComment,
                    data = API.User.Topic.GetLatestTopicsByComment.Request(
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
            if (result is Success) {
                state = if (page.newData(result.data)) CONTENT else EMPTY
                gridState.scrollToItem(0)
            }
            else state = NETWORK_ERROR
        }
    }

    private suspend fun requestMoreData() {
        val result = when (val section = currentSection) {
            DiscoveryItem.LatestTopic.id -> ClientAPI.request(
                route = API.User.Topic.GetLatestTopics,
                data = API.User.Topic.GetLatestTopics.Request(
                    tid = page.offset,
                    num = page.pageNum
                )
            )
            DiscoveryItem.LatestComment.id -> ClientAPI.request(
                route = API.User.Topic.GetLatestTopicsByComment,
                data = API.User.Topic.GetLatestTopicsByComment.Request(
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
        if (result is Success) page.moreData(result.data)
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
                    overflow = Ellipsis,
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
                            key = remember { DateEx.TodayString },
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
                            textAlign = Center,
                            maxLines = 1,
                            overflow = Ellipsis,
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
        requestNewData(true)
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = ThemeValue.Shadow.Surface
            ) {
                TabBar(
                    currentPage = currentPage,
                    onNavigate = {
                        currentPage = it
                        launch { requestNewData(true) }
                    },
                    items = DiscoveryItem.items,
                    modifier = Modifier.fillMaxWidth().padding(immersivePadding.withoutBottom)
                )
            }

            StatefulBox(
                state = state,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(immersivePadding.withoutTop)
            ) {
                bindPauseLoadWhenScrolling(gridState)
                PaginationStaggeredGrid(
                    items = page.items,
                    key = { it.tid },
                    columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CellWidth),
                    state = gridState,
                    canRefresh = true,
                    canLoading = page.canLoading,
                    onRefresh = { requestNewData(false) },
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

    override val fabCanExpand: Boolean get() = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0

    override val fabIcon: ImageVector? by derivedStateOf { if (fabCanExpand) Icons.Outlined.Add else Icons.Outlined.ArrowUpward }

    override val fabMenus: Array<FABAction> = arrayOf(
        FABAction(Icons.Outlined.Edit, "发表主题") {
            navigate<ScreenAddTopic>()
        },
        FABAction(Icons.Outlined.Refresh, "刷新") {
            launch { requestNewData(true) }
        }
    )

    override suspend fun onFabClick() {
        gridState.animateScrollToItem(0)
    }
}