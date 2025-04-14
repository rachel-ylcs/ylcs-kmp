package love.yinlin.ui.screen.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.platform.app
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen

@Stable
@Serializable
data class ScreenUserCard(val uid: Int) : Screen<ScreenUserCard.Model> {
	inner class Model(model: AppModel) : Screen.Model(model) {
		var profile: UserPublicProfile? by mutableStateOf(null)

		val listState = LazyStaggeredGridState()

		val page = object : PaginationArgs<Topic, Int, Boolean>(Int.MAX_VALUE, true) {
			override fun offset(item: Topic): Int = item.tid
			override fun arg1(item: Topic): Boolean = item.isTop
		}

		fun requestUserProfile() {
			launch {
				val result = ClientAPI.request(
					route = API.User.Profile.GetPublicProfile,
					data = uid
				)
				if (result is Data.Success) profile = result.data
			}
		}

		fun requestNewTopics() {
			launch {
				val result = ClientAPI.request(
					route = API.User.Topic.GetTopics,
					data = API.User.Topic.GetTopics.Request(
						uid = uid,
						num = page.pageNum
					)
				)
				if (result is Data.Success) page.newData(result.data)
			}
		}

		fun requestMoreTopics() {
			launch {
				val result = ClientAPI.request(
					route = API.User.Topic.GetTopics,
					data = API.User.Topic.GetTopics.Request(
						uid = uid,
						isTop = page.arg1,
						offset = page.offset,
						num = page.pageNum
					)
				)
				if (result is Data.Success) page.moreData(result.data)
			}
		}

		fun onTopicClick(topic: Topic) {
			navigate(ScreenTopic(topic))
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
					if (topic.isTop) {
						Row(
							modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 5.dp),
							horizontalArrangement = Arrangement.spacedBy(10.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							BoxText(text = "置顶", color = MaterialTheme.colorScheme.primary)
						}
					}
					Text(
						text = topic.title,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
					)
					Spacer(Modifier.weight(1f))
					Row(
						modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
						horizontalArrangement = Arrangement.spacedBy(5.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						RachelText(
							text = topic.commentNum.toString(),
							icon = Icons.AutoMirrored.Outlined.Comment,
							modifier = Modifier.weight(1f)
						)
						RachelText(
							text = topic.coinNum.toString(),
							icon = Icons.Outlined.Paid,
							modifier = Modifier.weight(1f)
						)
					}
				}
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model).apply {
		requestUserProfile()
		requestNewTopics()
	}

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "主页",
			onBack = { model.pop() },
			slot = model.slot
		) {
			model.profile?.let { profile ->
				val cardWidth = if (app.isPortrait) 150.dp else 180.dp
				if (app.isPortrait) {
					PaginationStaggeredGrid(
						items = model.page.items,
						key = { it.tid },
						columns = StaggeredGridCells.Adaptive(cardWidth),
						state = model.listState,
						canRefresh = false,
						canLoading = model.page.canLoading,
						onLoading = { model.requestMoreTopics() },
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(bottom = 10.dp),
						verticalItemSpacing = 10.dp,
						header = {
							Column(modifier = Modifier.fillMaxWidth()) {
								PortraitUserProfileCard(
									profile = profile,
									owner = false
								)
							}
						}
					) {  topic ->
						model.TopicCard(
							topic = topic,
							cardWidth = cardWidth,
							modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
						)
					}
				}
				else {
					Row(modifier = Modifier.fillMaxSize()) {
						LandscapeUserProfileCard(
							profile = profile,
							owner = false,
							modifier = Modifier.width(350.dp).padding(10.dp)
						)
						PaginationStaggeredGrid(
							items = model.page.items,
							key = { it.tid },
							columns = StaggeredGridCells.Adaptive(cardWidth),
							state = model.listState,
							canRefresh = false,
							canLoading = model.page.canLoading,
							onLoading = { model.requestMoreTopics() },
							modifier = Modifier.weight(1f).fillMaxHeight(),
							contentPadding = PaddingValues(10.dp),
							verticalItemSpacing = 10.dp
						) {  topic ->
							model.TopicCard(
								topic = topic,
								cardWidth = cardWidth,
								modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
							)
						}
					}
				}
			}
		}
	}
}