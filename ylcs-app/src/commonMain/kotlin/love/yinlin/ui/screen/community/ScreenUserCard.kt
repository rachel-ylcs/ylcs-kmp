package love.yinlin.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import love.yinlin.common.Device
import love.yinlin.data.Data
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationStaggeredGrid
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenUserCard(model: AppModel, private val args: Args) : SubScreen<ScreenUserCard.Args>(model) {
	@Stable
	@Serializable
	data class Args(val uid: Int)

	private var profile: UserPublicProfile? by mutableStateOf(null)

	private val listState = LazyStaggeredGridState()

	private val page = object : PaginationArgs<Topic, Int, Boolean>(Int.MAX_VALUE, true) {
		override fun offset(item: Topic): Int = item.tid
		override fun arg1(item: Topic): Boolean = item.isTop
	}

	private suspend fun requestUserProfile() {
		val result = ClientAPI.request(
			route = API.User.Profile.GetPublicProfile,
			data = args.uid
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
				offset = page.offset,
				num = page.pageNum
			)
		)
		if (result is Data.Success) page.moreData(result.data)
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

	@Composable
	private fun Portrait(profile: UserPublicProfile, cardWidth: Dp) {
		PaginationStaggeredGrid(
			items = page.items,
			key = { it.tid },
			columns = StaggeredGridCells.Adaptive(cardWidth),
			state = listState,
			canRefresh = false,
			canLoading = page.canLoading,
			onLoading = { requestMoreTopics() },
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(bottom = 10.dp),
			verticalItemSpacing = 10.dp,
			header = {
				PortraitUserProfileCard(
					modifier = Modifier.fillMaxWidth(),
					profile = profile,
					owner = false
				)
			}
		) {  topic ->
			TopicCard(
				topic = topic,
				cardWidth = cardWidth,
				modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
			)
		}
	}

	@Composable
	private fun Landscape(profile: UserPublicProfile, cardWidth: Dp) {
		Row(modifier = Modifier.fillMaxSize()) {
			LandscapeUserProfileCard(
				profile = profile,
				owner = false,
				modifier = Modifier.weight(1f).padding(20.dp)
			)
			PaginationStaggeredGrid(
				items = page.items,
				key = { it.tid },
				columns = StaggeredGridCells.Adaptive(180.dp),
				state = listState,
				canRefresh = false,
				canLoading = page.canLoading,
				onLoading = { requestMoreTopics() },
				modifier = Modifier.weight(1f).fillMaxHeight(),
				contentPadding = PaddingValues(10.dp),
				verticalItemSpacing = 10.dp
			) {  topic ->
				TopicCard(
					topic = topic,
					cardWidth = cardWidth,
					modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
				)
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
				Device.Type.PORTRAIT -> Portrait(it, 150.dp)
				Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(it, 180.dp)
			}
		} ?: EmptyBox()
	}
}