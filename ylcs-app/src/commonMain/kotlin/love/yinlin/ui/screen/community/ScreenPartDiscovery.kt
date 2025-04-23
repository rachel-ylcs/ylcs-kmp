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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.DateEx
import love.yinlin.platform.app
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.layout.TabBar
import love.yinlin.ui.component.screen.ActionScope

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
	var state by mutableStateOf(BoxState.EMPTY)

	val listState = LazyStaggeredGridState()

	var currentPage by mutableIntStateOf(0)
	val currentSection: Int get() = DiscoveryItem.entries[currentPage].id

	@Stable
	class DiscoveryPagination : Pagination<Topic, Int>(Int.MAX_VALUE) {
		var section = Comment.Section.LATEST
		override fun offset(item: Topic): Int = when (section) {
			DiscoveryItem.Latest.id -> item.tid
			DiscoveryItem.Hot.id -> item.coinNum
			else -> item.tid
		}
	}
	val page = DiscoveryPagination()

	suspend fun requestNewData() {
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
			page.section = section
			state = if (page.newData(result.data)) BoxState.CONTENT else BoxState.EMPTY
			listState.scrollToItem(0)
		}
		else state = BoxState.NETWORK_ERROR
	}

	suspend fun requestMoreData() {
		val section = currentSection
		val result = when (section) {
			DiscoveryItem.Latest.id -> ClientAPI.request(
				route = API.User.Topic.GetLatestTopics,
				data = API.User.Topic.GetLatestTopics.Request(
					offset = page.offset,
					num = page.pageNum
				)
			)
			DiscoveryItem.Hot.id -> ClientAPI.request(
				route = API.User.Topic.GetHotTopics,
				data = API.User.Topic.GetHotTopics.Request(
					offset = page.offset,
					num = page.pageNum
				)
			)
			else -> ClientAPI.request(
				route = API.User.Topic.GetSectionTopics,
				data = API.User.Topic.GetSectionTopics.Request(
					section = section,
					offset = page.offset,
					num = page.pageNum
				)
			)
		}
		if (result is Data.Success) {
			page.section = section
			page.moreData(result.data)
		}
	}

	fun onTopicClick(topic: Topic) {
		navigate(ScreenTopic.Args(topic))
	}

	fun onUserAvatarClick(uid: Int) {
		navigate(ScreenUserCard.Args(uid))
	}

	@Composable
	fun TopicCard(
		topic: Topic,
		cardWidth: Dp,
		modifier: Modifier = Modifier
	) {
		Surface(
			modifier = modifier,
			shape = MaterialTheme.shapes.large,
			shadowElevation = 3.dp
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
							modifier = Modifier.matchParentSize(),
							onClick = { onUserAvatarClick(topic.uid) }
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
							RachelText(
								text = topic.commentNum.toString(),
								icon = Icons.AutoMirrored.Outlined.Comment,
								style = MaterialTheme.typography.bodyMedium,
								padding = PaddingValues(0.dp),
								modifier = Modifier.weight(1f)
							)
							RachelText(
								text = topic.coinNum.toString(),
								icon = Icons.Outlined.Paid,
								style = MaterialTheme.typography.bodyMedium,
								padding = PaddingValues(0.dp),
								modifier = Modifier.weight(1f)
							)
						}
					}
				}
			}
		}
	}

	@Composable
	override fun content() {
		Column(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(5f),
				shadowElevation = 5.dp
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically
				) {
					TabBar(
						currentPage = currentPage,
						onNavigate = {
							currentPage = it
							launch { requestNewData() }
						},
						items = DiscoveryItem.items,
						modifier = Modifier.weight(1f).padding(end = 10.dp)
					)
					ActionScope.Right.Actions {
						Action(Icons.Outlined.Add) {
							navigate(ScreenAddTopic.Args)
						}
					}
				}
			}

			StatefulBox(
				state = state,
				modifier = Modifier.fillMaxSize()
			) {
				val cardWidth = if (app.isPortrait) 150.dp else 200.dp
				PaginationStaggeredGrid(
					items = page.items,
					key = { it.tid },
					columns = StaggeredGridCells.Adaptive(cardWidth),
					state = listState,
					canRefresh = true,
					canLoading = page.canLoading,
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
	}

	override suspend fun initialize() {
		requestNewData()
	}
}