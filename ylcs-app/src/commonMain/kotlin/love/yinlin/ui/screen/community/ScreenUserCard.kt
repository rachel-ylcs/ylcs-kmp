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
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
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
				tid = page.offset,
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
				if (topic.isTop) {
					Row(
						modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
						horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
						verticalAlignment = Alignment.CenterVertically
					) {
						BoxText(text = "置顶", color = MaterialTheme.colorScheme.primary)
					}
				}
				Text(
					text = topic.title,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)
				)
				Spacer(Modifier.weight(1f))
				Row(
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
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

	@Composable
	private fun Portrait(profile: UserPublicProfile) {
		PaginationStaggeredGrid(
			items = page.items,
			key = { it.tid },
			columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CellWidth),
			state = listState,
			canRefresh = false,
			canLoading = page.canLoading,
			onLoading = { requestMoreTopics() },
			modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
			verticalItemSpacing = ThemeValue.Padding.EqualSpace,
			header = {
				UserProfileCard(
					modifier = Modifier.fillMaxWidth(),
					profile = profile,
					owner = false
				)
			}
		) {  topic ->
			TopicCard(
				topic = topic,
				cardWidth = ThemeValue.Size.CellWidth,
				modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.EqualSpace / 2)
			)
		}
	}

	@Composable
	private fun Landscape(profile: UserPublicProfile) {
		Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
			UserProfileCard(
				profile = profile,
				owner = false,
				shape = MaterialTheme.shapes.large,
				modifier = Modifier.width(ThemeValue.Size.PanelWidth)
					.padding(ThemeValue.Padding.EqualExtraValue)
			)
			PaginationStaggeredGrid(
				items = page.items,
				key = { it.tid },
				columns = StaggeredGridCells.Adaptive(ThemeValue.Size.CellWidth),
				state = listState,
				canRefresh = false,
				canLoading = page.canLoading,
				onLoading = { requestMoreTopics() },
				modifier = Modifier.weight(1f).fillMaxHeight(),
				contentPadding = ThemeValue.Padding.EqualValue,
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
				verticalItemSpacing = ThemeValue.Padding.EqualSpace
			) {  topic ->
				TopicCard(
					topic = topic,
					cardWidth = ThemeValue.Size.CellWidth,
					modifier = Modifier.fillMaxWidth()
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
				Device.Type.PORTRAIT -> Portrait(it)
				Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(it)
			}
		} ?: EmptyBox()
	}
}