package love.yinlin.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.APIConfig
import love.yinlin.api.ClientAPI
import love.yinlin.common.ThemeColor
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.SubComment
import love.yinlin.data.rachel.Topic
import love.yinlin.data.rachel.TopicDetails
import love.yinlin.extension.DateEx
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.rememberState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.app
import love.yinlin.ui.component.button.RachelButton
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.PaginationColumn
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.RichString
import love.yinlin.ui.component.text.RichText
import love.yinlin.ui.screen.common.ScreenImagePreview

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
				key = DateEx.TodayString,
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
		UserLabel(label = label, level = level)
	}
}

@Stable
@Serializable
data class ScreenTopic(val currentTopic: Topic) : Screen<ScreenTopic.Model> {
	inner class Model(model: AppModel) : Screen.Model(model) {
		var details: TopicDetails? by mutableStateOf(null)
		var topic: Topic by mutableStateOf(currentTopic)

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

				commentCanLoading = data.size == APIConfig.MIN_PAGE_NUM
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

				commentCanLoading = commentOffset != 0 && data.size == APIConfig.MIN_PAGE_NUM
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
			part<ScreenPartDiscovery>().onUserAvatarClick(uid)
		}

		fun showSubComment(comment: Comment) {
			currentComment = comment
		}

		fun hideSubComment() {
			currentComment = null
		}

		fun onImageClick(images: List<Picture>, current: Int) {
			navigate(ScreenImagePreview(images, current))
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

		@Composable
		private fun TopicLayout(details: TopicDetails, modifier: Modifier = Modifier) {
			val pics by rememberDerivedState { details.pics.map { Picture(topic.picPath(it)) } }

			Column(
				modifier = modifier,
				verticalArrangement = Arrangement.spacedBy(5.dp)
			) {
				UserBar(
					avatar = topic.avatarPath,
					name = topic.name,
					time = details.ts,
					label = details.label,
					level = details.level,
					onAvatarClick = { onAvatarClick(topic.uid) }
				)
				Text(
					text = topic.title,
					style = MaterialTheme.typography.titleMedium,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.fillMaxWidth()
				)
				RichText(
					text = remember(details) { RichString.parse(details.content) },
					modifier = Modifier.fillMaxWidth()
				)
				if (pics.isNotEmpty()) {
					NineGrid(
						modifier = Modifier.fillMaxWidth(),
						pics = pics,
						onImageClick = { onImageClick(pics, it) },
						onVideoClick = {}
					)
				}
				Row(
					modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
					horizontalArrangement = Arrangement.SpaceAround,
					verticalAlignment = Alignment.CenterVertically
				) {
					RachelButton(
						text = "评论",
						icon = Icons.Filled.AddComment,
						onClick = { onSendComment() }
					)
					RachelButton(
						text = "投币",
						icon = Icons.Filled.Paid,
						onClick = { onSendCoin() }
					)
					app.config.userProfile?.let { user ->
						if (user.canUpdateTopicTop(topic.uid)) {
							RachelButton(
								text = if (topic.isTop) "取消置顶" else "置顶",
								icon = if (topic.isTop) Icons.Filled.Close else Icons.Filled.VerticalAlignTop,
								onClick = { onChangeTopicIsTop(!topic.isTop) }
							)
						}
						if (user.canDeleteTopic(topic.uid)) {
							RachelButton(
								text = "删除",
								icon = Icons.Filled.Delete,
								onClick = { onSendCoin() }
							)
						}
					}
				}
			}
		}

		@Composable
		private fun CommentBar(comment: Comment, modifier: Modifier = Modifier) {
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
					onAvatarClick = { onAvatarClick(comment.uid) }
				)
				Row(
					horizontalArrangement = Arrangement.spacedBy(5.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					if (comment.isTop) BoxText(text = "置顶", color = MaterialTheme.colorScheme.primary)
					if (comment.uid == topic.uid) BoxText(text = "楼主", color = MaterialTheme.colorScheme.secondary)
				}
				RichText(
					text = remember(comment) { RichString.parse(comment.content) },
					modifier = Modifier.fillMaxWidth()
				)
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
								modifier = Modifier.clickable { showSubComment(comment) }.padding(2.dp)
							)
						}
					}
					Text(
						text = "回复",
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.secondary,
						modifier = Modifier.clickable { onSendSubComment(comment.cid) }.padding(2.dp)
					)
					app.config.userProfile?.let { user ->
						if (user.canUpdateCommentTop(topic.uid)) {
							Text(
								text = if (comment.isTop) "取消置顶" else "置顶",
								style = MaterialTheme.typography.labelLarge,
								modifier = Modifier.clickable { onChangeCommentIsTop(comment.cid, !comment.isTop) }.padding(2.dp)
							)
						}
						if (user.canDeleteComment(topic.uid, comment.uid)) {
							Text(
								text = "删除",
								style = MaterialTheme.typography.labelLarge,
								modifier = Modifier.clickable { onDeleteComment(comment.cid) } .padding(2.dp)
							)
						}
					}
				}
			}
		}

		@Composable
		private fun SubCommentBar(
			subComment: SubComment,
			commentUid: Int,
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
					onAvatarClick = {
						hideSubComment()
						onAvatarClick(subComment.uid)
					}
				)
				Row(
					modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
					horizontalArrangement = Arrangement.spacedBy(5.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					if (subComment.uid == topic.uid) BoxText(text = "楼主", color = MaterialTheme.colorScheme.secondary)
					if (subComment.uid == commentUid) BoxText(text = "层主", color = MaterialTheme.colorScheme.tertiary)
					app.config.userProfile?.let { user ->
						if (user.canDeleteComment(topic.uid, subComment.uid)) {
							Row(
								modifier = Modifier.weight(1f),
								horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
								verticalAlignment = Alignment.CenterVertically
							) {
								Text(
									text = "删除",
									textAlign = TextAlign.End,
									style = MaterialTheme.typography.labelLarge,
									modifier = Modifier.clickable { onDeleteSubComment(subComment.cid) }.padding(2.dp)
								)
							}
						}
					}
				}
				RichText(
					text = remember(subComment) { RichString.parse(subComment.content) },
					modifier = Modifier.fillMaxWidth()
				)
			}
		}

		@OptIn(ExperimentalMaterial3Api::class)
		@Composable
		fun SubCommentLayout(comment: Comment, modifier: Modifier = Modifier) {
			val subComments = remember { mutableStateListOf<SubComment>() }
			var subCommentOffset: Int = remember { 0 }
			var subCommentCanLoading by rememberState { false }

			ModalBottomSheet(
				onDismissRequest = { hideSubComment() },
				dragHandle = null,
				modifier = modifier
			) {
				PaginationColumn(
					items = subComments,
					key = { it.cid },
					canRefresh = false,
					canLoading = subCommentCanLoading,
					onLoading = {
						requestSubComments(
							cid = comment.cid,
							offset = subCommentOffset
						)?.let { data ->
							if (data.isEmpty()) subCommentOffset = 0
							else {
								subComments += data
								val last = data.lastOrNull()
								subCommentOffset = last?.cid ?: 0
							}

							subCommentCanLoading = subCommentOffset != 0 && data.size == APIConfig.MIN_PAGE_NUM
						}
					},
					contentPadding = PaddingValues(top = 10.dp),
					itemDivider = PaddingValues(vertical = 8.dp),
					modifier = Modifier.fillMaxWidth()
				) {
					SubCommentBar(
						subComment = it,
						commentUid = comment.uid,
						modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
					)
				}
			}

			LaunchedEffect(Unit) {
				requestSubComments(
					cid = comment.cid,
					offset = subCommentOffset
				)?.let { data ->
					subComments.replaceAll(data)

					val last = data.lastOrNull()
					subCommentOffset = last?.cid ?: 0

					subCommentCanLoading = data.size == APIConfig.MIN_PAGE_NUM
				}
			}
		}

		@Composable
		fun Portrait(details: TopicDetails) {
			PaginationColumn(
				items = comments,
				key = { it.cid },
				canRefresh = false,
				canLoading = commentCanLoading,
				onLoading = { requestMoreComments() },
				modifier = Modifier.fillMaxSize(),
				header = {
					TopicLayout(
						details = details,
						modifier = Modifier.fillMaxWidth().padding(10.dp)
					)
					HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))
				},
				itemDivider = PaddingValues(vertical = 8.dp)
			) {
				CommentBar(
					comment = it,
					modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
				)
			}
		}

		@Composable
		fun Landscape(details: TopicDetails) {
			Row(modifier = Modifier.fillMaxSize()) {
				TopicLayout(
					modifier = Modifier.width(400.dp).fillMaxHeight().padding(horizontal = 5.dp, vertical = 10.dp)
						.verticalScroll(rememberScrollState()),
					details = details
				)
				VerticalDivider()
				PaginationColumn(
					items = comments,
					key = { it.cid },
					canRefresh = false,
					canLoading = commentCanLoading,
					onLoading = { requestMoreComments() },
					itemDivider = PaddingValues(vertical = 8.dp),
					modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 5.dp, vertical = 10.dp)
				) {
					CommentBar(
						comment = it,
						modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
					)
				}
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model).apply {
		launch {
			requestTopic()
			requestNewComments()
		}
	}

	@Composable
	override fun content(model: Model) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "主题",
			onBack = {
				if (model.currentComment != null) model.hideSubComment()
				else model.pop()
			}
		) {
			val details = model.details
			if (details == null) EmptyBox()
			else if (app.isPortrait) model.Portrait(details = details)
			else model.Landscape(details = details)
		}

		model.currentComment?.let {
			model.SubCommentLayout(
				comment = it,
				modifier = Modifier.fillMaxSize()
			)
		}

		DisposableEffect(Unit) {
			onDispose {
				model.hideSubComment()
			}
		}
	}
}