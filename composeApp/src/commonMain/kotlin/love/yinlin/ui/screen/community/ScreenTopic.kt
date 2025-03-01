package love.yinlin.ui.screen.community

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.AppModel
import love.yinlin.ThemeColor
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.SubComment
import love.yinlin.data.rachel.Topic
import love.yinlin.data.rachel.TopicDetails
import love.yinlin.extension.DateEx
import love.yinlin.extension.LaunchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.rememberStateSaveable
import love.yinlin.extension.replaceAll
import love.yinlin.platform.app
import love.yinlin.platform.config
import love.yinlin.ui.Route
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.PaginationColumn
import love.yinlin.ui.component.screen.SubScreen
import kotlin.collections.plusAssign

private class TopicModel(private val model: AppModel, topic: Topic) : ViewModel() {
	val launchFlag = LaunchFlag()

	var details: TopicDetails? by mutableStateOf(null)
	var topic: Topic by mutableStateOf(topic)

	val comments = mutableStateListOf<Comment>()
	var commentOffset: Int = 0
	var commentIsTop: Boolean = true
	var commentCanLoading by mutableStateOf(false)

	var currentComment: Comment? by mutableStateOf(null)

	suspend fun requestTopic() {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicDetails,
			data = topic.tid
		)
		if (result is Data.Success) details = result.data
	}

	suspend fun requestNewComments() {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicComments,
			data = API.User.Topic.GetTopicComments.Request(
				tid = topic.tid,
				rawSection = topic.rawSection
			)
		)
		if (result is Data.Success) {
			val data = result.data
			comments.replaceAll(data)

			val last = data.lastOrNull()
			commentOffset = last?.cid ?: 0
			commentIsTop = last?.isTop ?: true

			commentCanLoading = data.isNotEmpty()
		}
	}

	suspend fun requestMoreComments() {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicComments,
			data = API.User.Topic.GetTopicComments.Request(
				tid = topic.tid,
				rawSection = topic.rawSection,
				offset = commentOffset,
				isTop = commentIsTop
			)
		)
		if (result is Data.Success) {
			val data = result.data
			if (data.isEmpty()) {
				commentOffset = 0
				commentIsTop = true
			}
			else {
				comments += data
				val last = data.lastOrNull()
				commentOffset = last?.cid ?: 0
				commentIsTop = last?.isTop ?: true
			}

			commentCanLoading = commentOffset != 0
		}
	}

	suspend fun requestSubComments(cid: Int, offset: Int): List<SubComment>? {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicSubComments,
			data = API.User.Topic.GetTopicSubComments.Request(
				cid = cid,
				rawSection = topic.rawSection,
				offset = offset
			)
		)
		return if (result is Data.Success) result.data else null
	}

	fun onAvatarClick(uid: Int) {
		model.mainModel.discoveryModel.onUserAvatarClick(uid)
	}

	fun showSubComment(comment: Comment) {
		currentComment = comment
	}

	fun hideSubComment() {
		currentComment = null
	}

	fun onImageClick(images: List<Picture>, current: Int) {
		model.mainModel.navigate(Route.ImagePreview(images, current))
	}

	fun onSendComment() {

	}

	fun onSendCoin() {

	}

	fun onSendSubComment(cid: Int) {

	}

	fun onChangeTopicIsTop(value: Boolean) {

	}

	fun onDeleteTopic() {

	}

	fun onChangeCommentIsTop(cid: Int, value: Boolean) {

	}

	fun onDeleteComment(cid: Int) {

	}

	fun onDeleteSubComment(cid: Int) {

	}
}

@Composable
private fun UserBar(
	avatar: String,
	name: String,
	time: String,
	label: String,
	level: Int,
	onAvatarClick: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
			WebImage(
				uri = avatar,
				key = DateEx.currentDateString,
				contentScale = ContentScale.Crop,
				circle = true,
				onClick = onAvatarClick,
				modifier = Modifier.matchParentSize()
			)
		}
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			Text(
				text = name,
				style = MaterialTheme.typography.labelLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth()
			)
			Text(
				text = time,
				color = ThemeColor.fade,
				style = MaterialTheme.typography.bodyMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth()
			)
		}
		UserLabel(
			label = label,
			level = level
		)
	}
}

@Composable
private fun CommandButton(
	text: String,
	icon: ImageVector,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier.height(IntrinsicSize.Min).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp),
		horizontalArrangement = Arrangement.spacedBy(5.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				modifier = Modifier.matchParentSize()
			)
		}
		Text(text = text)
	}
}

@Composable
private fun TopicLayout(
	details: TopicDetails,
	model: TopicModel,
	modifier: Modifier = Modifier
) {
	val pics = remember(details) { details.pics.map { Picture(model.topic.picPath(it)) } }

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(5.dp)
	) {
		UserBar(
			avatar = model.topic.avatarPath,
			name = model.topic.name,
			time = details.ts,
			label = details.label,
			level = details.level,
			onAvatarClick = { model.onAvatarClick(model.topic.uid) }
		)
		Text(
			text = model.topic.title,
			style = MaterialTheme.typography.titleMedium,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.fillMaxWidth()
		)
		Text(
			text = details.content,
			modifier = Modifier.fillMaxWidth()
		)
		if (pics.isNotEmpty()) {
			NineGrid(
				modifier = Modifier.fillMaxWidth(),
				pics = pics,
				onImageClick = { model.onImageClick(pics, it) },
				onVideoClick = {}
			)
		}
		Row(
			modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
			horizontalArrangement = Arrangement.SpaceAround,
			verticalAlignment = Alignment.CenterVertically
		) {
			CommandButton(
				text = "评论",
				icon = Icons.Filled.AddComment,
				onClick = { model.onSendComment() }
			)
			CommandButton(
				text = "投币",
				icon = Icons.Filled.Paid,
				onClick = { model.onSendCoin() }
			)

			config.userProfile?.let { user ->
				if (user.canUpdateTopicTop(model.topic.uid)) {
					CommandButton(
						text = if (model.topic.isTop) "取消置顶" else "置顶",
						icon = if (model.topic.isTop) Icons.Filled.Close else Icons.Filled.VerticalAlignTop,
						onClick = { model.onChangeTopicIsTop(!model.topic.isTop) }
					)
				}
				if (user.canDeleteTopic(model.topic.uid)) {
					CommandButton(
						text = "删除",
						icon = Icons.Filled.Delete,
						onClick = { model.onSendCoin() }
					)
				}
			}
		}
	}
}

@Composable
private fun CommentFlag(
	text: String,
	color: Color
) {
	Box(
		modifier = Modifier.padding(vertical = 3.dp).border(1.dp, color = color),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = text,
			style = MaterialTheme.typography.labelMedium,
			color = color,
			modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
		)
	}
}

@Composable
private fun CommentBar(
	comment: Comment,
	topicUid: Int,
	onAvatarClick: () -> Unit,
	onSubCommentClick: () -> Unit,
	onSendSubComment: () -> Unit,
	onChangeIsTop: () -> Unit,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(3.dp)
	) {
		UserBar(
			avatar = comment.avatarPath,
			name = comment.name,
			time = comment.ts,
			label = comment.label,
			level = comment.level,
			onAvatarClick = onAvatarClick
		)
		Row(
			horizontalArrangement = Arrangement.spacedBy(5.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (comment.isTop) CommentFlag(text = "置顶", color = MaterialTheme.colorScheme.primary)
			if (comment.uid == topicUid) CommentFlag(text = "楼主", color = MaterialTheme.colorScheme.secondary)
		}
		Text(text = comment.content)
		Row(
			modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
			horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (comment.subCommentNum > 0) {
				Box(modifier = Modifier.weight(1f)) {
					Text(
						text = ">> 查看${comment.subCommentNum}条回复",
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier.clickable(onClick = onSubCommentClick)
					)
				}
			}
			Text(
				text = "回复",
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.secondary,
				modifier = Modifier.clickable(onClick = onSendSubComment)
			)
			config.userProfile?.let { user ->
				if (user.canUpdateCommentTop(topicUid)) {
					Text(
						text = if (comment.isTop) "取消置顶" else "置顶",
						style = MaterialTheme.typography.labelLarge,
						modifier = Modifier.clickable(onClick = onChangeIsTop)
					)
				}
				if (user.canDeleteComment(topicUid, comment.uid)) {
					Text(
						text = "删除",
						style = MaterialTheme.typography.labelLarge,
						modifier = Modifier.clickable(onClick = onDelete)
					)
				}
			}
		}
	}
}

@Composable
private fun SubCommentBar(
	subComment: SubComment,
	topicUid: Int,
	commentUid: Int,
	onAvatarClick: () -> Unit,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(3.dp)
	) {
		UserBar(
			avatar = subComment.avatarPath,
			name = subComment.name,
			time = subComment.ts,
			label = subComment.label,
			level = subComment.level,
			onAvatarClick = onAvatarClick
		)
		Row(
			modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
			horizontalArrangement = Arrangement.spacedBy(5.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (subComment.uid == topicUid) CommentFlag(text = "楼主", color = MaterialTheme.colorScheme.secondary)
			if (subComment.uid == commentUid) CommentFlag(text = "层主", color = MaterialTheme.colorScheme.tertiary)
			config.userProfile?.let { user ->
				if (user.canDeleteComment(topicUid, subComment.uid)) {
					Row(
						modifier = Modifier.weight(1f),
						horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = "删除",
							textAlign = TextAlign.End,
							style = MaterialTheme.typography.labelLarge,
							modifier = Modifier.clickable(onClick = onDelete)
						)
					}
				}
			}
		}
		Text(text = subComment.content)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubCommentLayout(
	model: TopicModel,
	comment: Comment,
	modifier: Modifier = Modifier,
) {
	val subComments = remember { mutableStateListOf<SubComment>() }
	var subCommentOffset: Int = remember { 0 }
	var subCommentCanLoading by rememberSaveable { mutableStateOf(false) }

	ModalBottomSheet(
		onDismissRequest = { model.hideSubComment() },
		dragHandle = null,
		modifier = modifier
	) {
		PaginationColumn(
			items = subComments,
			key = { it.cid },
			canRefresh = false,
			canLoading = subCommentCanLoading,
			onLoading = {
				model.requestSubComments(
					cid = comment.cid,
					offset = subCommentOffset
				)?.let { data ->
					if (data.isEmpty()) subCommentOffset = 0
					else {
						subComments += data
						val last = data.lastOrNull()
						subCommentOffset = last?.cid ?: 0
					}

					subCommentCanLoading = subCommentOffset != 0
				}
			},
			contentPadding = PaddingValues(top = 10.dp),
			modifier = Modifier.fillMaxWidth()
		) {
			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
			SubCommentBar(
				subComment = it,
				topicUid = model.topic.uid,
				commentUid = comment.uid,
				onAvatarClick = { model.onAvatarClick(it.uid) },
				onDelete = { model.onDeleteSubComment(it.cid) },
				modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
			)
		}
	}

	LaunchedEffect(Unit) {
		model.requestSubComments(
			cid = comment.cid,
			offset = subCommentOffset
		)?.let { data ->
			subComments.replaceAll(data)

			val last = data.lastOrNull()
			subCommentOffset = last?.cid ?: 0

			subCommentCanLoading = data.isNotEmpty()
		}
	}
}

@Composable
private fun Portrait(
	model: TopicModel,
	details: TopicDetails
) {
	PaginationColumn(
		items = model.comments,
		key = { it.cid },
		canRefresh = false,
		canLoading = model.commentCanLoading,
		onLoading = { model.requestMoreComments() },
		modifier = Modifier.fillMaxSize(),
		header = {
			TopicLayout(
				model = model,
				details = details,
				modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp)
			)
		}
	) {
		HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
		CommentBar(
			comment = it,
			topicUid = model.topic.uid,
			onAvatarClick = { model.onAvatarClick(it.uid) },
			onSubCommentClick = { model.showSubComment(it) },
			onSendSubComment = { model.onSendSubComment(it.cid) },
			onChangeIsTop = { model.onChangeCommentIsTop(it.cid, !it.isTop) },
			onDelete = { model.onDeleteComment(it.cid) },
			modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
		)
	}

	model.currentComment?.let {
		SubCommentLayout(
			model = model,
			comment = it,
			modifier = Modifier.fillMaxSize()
		)
	}
}

@Composable
private fun Landscape(
	model: TopicModel,
	details: TopicDetails
) {
	Row(modifier = Modifier.fillMaxSize()) {
		TopicLayout(
			modifier = Modifier.width(400.dp).fillMaxHeight()
				.verticalScroll(rememberScrollState()),
			model = model,
			details = details
		)
		VerticalDivider(modifier = Modifier.padding(horizontal = 10.dp))
	}
}

@Composable
fun ScreenTopic(model: AppModel, topic: Topic) {
	val screenModel = viewModel { TopicModel(model, topic) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "主题",
		onBack = {
			if (screenModel.currentComment != null) screenModel.hideSubComment()
			else model.pop()
		}
	) {
		val details = screenModel.details
		if (details == null) EmptyBox()
		else if (app.isPortrait) Portrait(
			model = screenModel,
			details = details
		)
		else Landscape(
			model = screenModel,
			details = details
		)
	}

	LaunchOnce(screenModel.launchFlag) {
		screenModel.requestTopic()
		screenModel.requestNewComments()
	}
}