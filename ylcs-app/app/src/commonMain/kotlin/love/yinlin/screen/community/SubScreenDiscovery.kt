package love.yinlin.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.Serializable
import love.yinlin.api.*
import love.yinlin.compose.*
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.DateEx
import love.yinlin.compose.ui.container.TabBar
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid

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
class SubScreenDiscovery(parent: BasicScreen) : SubScreen(parent) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private val gridState = LazyStaggeredGridState()

    private var currentPage by mutableIntStateOf(0)
    val currentSection: Int by derivedStateOf { DiscoveryItem.entries[currentPage].id }

    val page = object : PaginationArgs<Topic, Int, Int, Double>(
        default = Int.MAX_VALUE,
        default1 = Double.MAX_VALUE,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: Topic): Int = item.tid
        override fun offset(item: Topic): Int = item.tid
        override fun arg1(item: Topic): Double = item.score
    }

    private suspend fun requestNewData(loading: Boolean) {
        if (state != BoxState.LOADING) {
            if (loading) state = BoxState.LOADING
            var data: List<Topic>? = null
            when (val section = currentSection) {
                DiscoveryItem.LatestTopic.id -> ApiTopicGetLatestTopics.request(page.default, page.pageNum) { data = it }
                DiscoveryItem.LatestComment.id -> ApiTopicGetLatestTopicsByComment.request(page.default, page.pageNum) { data = it }
                DiscoveryItem.Hot.id -> ApiTopicGetHotTopics.request(page.default1, page.default, page.pageNum) { data = it }
                else -> ApiTopicGetSectionTopics.request(section, page.default, page.pageNum) { data = it }
            }
            if (data != null) {
                state = if (page.newData(data)) BoxState.CONTENT else BoxState.EMPTY
                gridState.scrollToItem(0)
            }
            else state = BoxState.NETWORK_ERROR
        }
    }

    private suspend fun requestMoreData() {
        var data: List<Topic>? = null
        when (val section = currentSection) {
            DiscoveryItem.LatestTopic.id -> ApiTopicGetLatestTopics.request(page.offset, page.pageNum) { data = it }
            DiscoveryItem.LatestComment.id -> ApiTopicGetLatestTopicsByComment.request(page.offset, page.pageNum) { data = it }
            DiscoveryItem.Hot.id -> ApiTopicGetHotTopics.request(page.arg1, page.offset, page.pageNum) { data = it }
            else -> ApiTopicGetSectionTopics.request(section, page.offset, page.pageNum) { data = it }
        }
        if (data != null) page.moreData(data)
    }

    private fun onTopicClick(topic: Topic) {
        navigate(::ScreenTopic, topic)
    }

    fun onUserAvatarClick(uid: Int) {
        navigate(::ScreenUserCard, uid)
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
                topic.picPath?.url?.let {
                    WebImage(
                        uri = it,
                        modifier = Modifier.fillMaxWidth().height(cardWidth * 1.333333f),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = topic.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value)
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(CustomTheme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
                        WebImage(
                            uri = remember(topic) { topic.avatarPath.url },
                            key = remember { DateEx.TodayString },
                            contentScale = ContentScale.Crop,
                            circle = true,
                            modifier = Modifier.matchParentSize(),
                            onClick = { onUserAvatarClick(topic.uid) }
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
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
        }
    }

    override suspend fun initialize() {
        requestNewData(true)
    }

    @Composable
    override fun Content(device: Device) {
        Column(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = CustomTheme.shadow.surface
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
                PauseLoading(gridState)

                PaginationStaggeredGrid(
                    items = page.items,
                    key = { it.tid },
                    columns = StaggeredGridCells.Adaptive(CustomTheme.size.cellWidth),
                    state = gridState,
                    canRefresh = true,
                    canLoading = page.canLoading,
                    onRefresh = { requestNewData(false) },
                    onLoading = { requestMoreData() },
                    modifier = Modifier.fillMaxSize(),
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

    override val fabCanExpand: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector? by derivedStateOf { if (fabCanExpand) Icons.Outlined.Add else Icons.Outlined.ArrowUpward }

    override val fabMenus: Array<FABAction> = arrayOf(
        FABAction(Icons.Outlined.Edit, "发表主题") {
            navigate(::ScreenAddTopic)
        },
        FABAction(Icons.Outlined.Refresh, "刷新") {
            launch { requestNewData(true) }
        }
    )

    override suspend fun onFabClick() {
        gridState.animateScrollToItem(0)
    }
}