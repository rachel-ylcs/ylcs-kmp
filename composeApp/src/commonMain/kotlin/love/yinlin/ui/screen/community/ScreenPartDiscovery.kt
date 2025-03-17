package love.yinlin.ui.screen.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.Topic
import love.yinlin.extension.DateEx
import love.yinlin.extension.launchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.replaceAll
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.layout.TabBar

private enum class DiscoveryItem(
	val id: Int,
	val title: String,
	val icon: ImageVector
) {
	Lastest(Comment.Section.LATEST, "最新", Icons.Filled.NewReleases),
	Hot(Comment.Section.HOT, "热门", Icons.Filled.LocalFireDepartment),
	Notification(Comment.Section.NOTIFICATION, "公告", Icons.Filled.Campaign),
	Water(Comment.Section.WATER, "水贴", Icons.Filled.WaterDrop),
	Activity(Comment.Section.ACTIVITY, "活动", Icons.Filled.Celebration),
	Discussion(Comment.Section.DISCUSSION, "交流", Icons.AutoMirrored.Filled.Chat);

	companion object {
		@Stable
		val items = DiscoveryItem.entries.map { it.title to it.icon }
	}
}

class ScreenPartDiscovery(model: AppModel) : ScreenPart(model) {
	val flagFirstLoad = launchFlag()
	var state by mutableStateOf(BoxState.EMPTY)

	val listState = LazyStaggeredGridState()

	var currentPage by mutableIntStateOf(0)
	val items = mutableStateListOf<Topic>()
	var offset: Int = Int.MAX_VALUE
	var canLoading by mutableStateOf(false)

	suspend fun requestNewData() {
		val section = DiscoveryItem.entries[currentPage].id
		state = BoxState.LOADING
		val result = when (currentPage) {
			DiscoveryItem.Lastest.ordinal -> ClientAPI.request(
				route = API.User.Topic.GetLatestTopics,
				data = API.User.Topic.GetLatestTopics.Request()
			)
			DiscoveryItem.Hot.ordinal -> ClientAPI.request(
				route = API.User.Topic.GetHotTopics,
				data = API.User.Topic.GetHotTopics.Request()
			)
			else -> ClientAPI.request(
				route = API.User.Topic.GetSectionTopics,
				data = API.User.Topic.GetSectionTopics.Request(section = section)
			)
		}
		if (result is Data.Success) {
			val topics = result.data
			items.replaceAll(topics)

			val last = topics.lastOrNull()
			offset = when (section) {
				DiscoveryItem.Lastest.id -> last?.tid
				DiscoveryItem.Hot.id -> last?.coinNum
				else -> last?.tid
			} ?: Int.MAX_VALUE

			if (topics.isEmpty()) {
				state = BoxState.EMPTY
				canLoading = false
			}
			else {
				state = BoxState.CONTENT
				canLoading = true
			}

			listState.scrollToItem(0)
		}
		else state = BoxState.NETWORK_ERROR
	}

	suspend fun requestMoreData() {
		val section = DiscoveryItem.entries[currentPage].id
		val result = when (currentPage) {
			DiscoveryItem.Lastest.ordinal -> ClientAPI.request(
				route = API.User.Topic.GetLatestTopics,
				data = API.User.Topic.GetLatestTopics.Request(offset = offset)
			)
			DiscoveryItem.Hot.ordinal -> ClientAPI.request(
				route = API.User.Topic.GetHotTopics,
				data = API.User.Topic.GetHotTopics.Request(offset = offset)
			)
			else -> ClientAPI.request(
				route = API.User.Topic.GetSectionTopics,
				data = API.User.Topic.GetSectionTopics.Request(section = section, offset = offset)
			)
		}
		if (result is Data.Success) {
			val topics = result.data
			if (topics.isEmpty()) offset = Int.MAX_VALUE
			else {
				items += topics
				val last = topics.lastOrNull()
				offset = when (section) {
					DiscoveryItem.Lastest.id -> last?.tid
					DiscoveryItem.Hot.id -> last?.coinNum
					else -> last?.tid
				} ?: Int.MAX_VALUE
			}

			canLoading = offset != Int.MAX_VALUE
		}
	}

	fun onRefresh() {
		launch { requestNewData() }
	}

	fun onTopicClick(topic: Topic) {
		navigate(ScreenTopic(topic))
	}

	fun onUserAvatarClick(uid: Int) {

	}

	@Composable
	fun TopicCard(
		topic: Topic,
		cardWidth: Dp,
		modifier: Modifier = Modifier
	) {
		ElevatedCard(
			modifier = modifier,
			colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
			onClick = { onTopicClick(topic) }
		) {
			Column(modifier = Modifier.fillMaxWidth().heightIn(min = cardWidth * 0.777777f)) {
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
					modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
				)
				Spacer(Modifier.weight(1f))
				Row(
					modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(5.dp),
					horizontalArrangement = Arrangement.spacedBy(5.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
						WebImage(
							uri = topic.avatarPath,
							key = DateEx.TodayString,
							contentScale = ContentScale.Crop,
							circle = true,
							modifier = Modifier.matchParentSize()
						)
					}
					Column(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(5.dp)
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
							horizontalArrangement = Arrangement.spacedBy(5.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Row(
								modifier = Modifier.weight(1f),
								horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
								verticalAlignment = Alignment.CenterVertically
							) {
								MiniIcon(
									imageVector = Icons.AutoMirrored.Outlined.Comment,
									size = 16.dp
								)
								Text(
									text = topic.commentNum.toString(),
									style = MaterialTheme.typography.bodyMedium,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis
								)
							}
							Row(
								modifier = Modifier.weight(1f),
								horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
								verticalAlignment = Alignment.CenterVertically
							) {
								MiniIcon(
									imageVector = Icons.Outlined.Paid,
									size = 16.dp
								)
								Text(
									text = topic.coinNum.toString(),
									style = MaterialTheme.typography.bodyMedium,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis
								)
							}
						}
					}
				}
			}
		}
	}

	@Composable
	override fun content() {
		StatefulBox(
			state = state,
			modifier = Modifier.fillMaxSize()
		) {
			Column(modifier = Modifier.fillMaxSize()) {
				Surface(
					modifier = Modifier.fillMaxWidth().zIndex(5f),
					shadowElevation = 5.dp
				) {
					Row(
						modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(10.dp)
					) {
						TabBar(
							currentPage = currentPage,
							onNavigate = {
								currentPage = it
								onRefresh()
							},
							items = DiscoveryItem.items,
							modifier = Modifier.weight(1f)
						)
						ClickIcon(
							imageVector = Icons.Outlined.Add,
							onClick = { }
						)
						ClickIcon(
							imageVector = Icons.Outlined.Search,
							onClick = { }
						)
						ClickIcon(
							imageVector = Icons.Outlined.Refresh,
							onClick = { onRefresh() }
						)
					}
				}

				val cardWidth = if (app.isPortrait) 150.dp else 200.dp
				PaginationStaggeredGrid(
					items = items,
					key = { it.tid },
					columns = StaggeredGridCells.Adaptive(cardWidth),
					state = listState,
					canRefresh = true,
					canLoading = canLoading,
					onRefresh = { requestNewData() },
					onLoading = { requestMoreData() },
					modifier = Modifier.fillMaxWidth().weight(1f),
					contentPadding = PaddingValues(10.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalItemSpacing = 10.dp
				) { topic ->
					TopicCard(
						topic = topic,
						cardWidth = cardWidth,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}

		LaunchOnce(flagFirstLoad) {
			onRefresh()
		}
	}
}