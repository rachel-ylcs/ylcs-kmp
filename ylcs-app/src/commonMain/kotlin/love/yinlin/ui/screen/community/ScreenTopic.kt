package love.yinlin.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.SubComment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.topic.TopicDetails
import love.yinlin.extension.DateEx
import love.yinlin.extension.findAssign
import love.yinlin.extension.rememberDerivedState
import love.yinlin.platform.UnsupportedComponent
import love.yinlin.platform.app
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.screen.*
import love.yinlin.ui.component.text.RichEditor
import love.yinlin.ui.component.text.RichEditorState
import love.yinlin.ui.component.text.RichString
import love.yinlin.ui.component.text.RichText
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.common.ScreenWebpage.Companion.gotoWebPage

@Composable
private fun CoinLayout(
	num: Int,
	modifier: Modifier = Modifier,
	onClick: (Int) -> Unit
) {
	Surface(
		modifier = modifier,
		shape = MaterialTheme.shapes.large,
		tonalElevation = ThemeValue.Shadow.Tonal,
		shadowElevation = ThemeValue.Shadow.Surface
	) {
		Column(
			modifier = Modifier.clickable{ onClick(num) }.padding(ThemeValue.Padding.EqualValue),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			Row (
				modifier = Modifier.fillMaxWidth().weight(1f),
				horizontalArrangement = Arrangement.Center
			) {
				repeat(num) {
					MiniIcon(
						icon = Icons.Filled.Paid,
						color = when (num) {
							1 -> MaterialTheme.colorScheme.tertiary
							2 -> MaterialTheme.colorScheme.secondary
							else -> MaterialTheme.colorScheme.primary
						},
						size = ThemeValue.Size.MediumIcon * (1 - num / 20f)
					)
				}
			}
			if (num == UserConstraint.MIN_COIN_REWARD) {
				Text(
					text = "作者获赠1银币",
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					style = MaterialTheme.typography.bodySmall,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
			Text(
				text = "$num 银币",
				style = MaterialTheme.typography.bodyLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Stable
private data class AtInfo(val uid: Int, val name: String) {
	override fun equals(other: Any?): Boolean = other is AtInfo && other.uid == uid
	override fun hashCode(): Int = uid

	val avatarPath: String by lazy { "${Local.API_BASE_URL}/${ServerRes.Users.User(uid).avatar}" }
}

@Composable
private fun AtUserItem(
	info: AtInfo,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.clickable(onClick = onClick).padding(ThemeValue.Padding.Value),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace)
	) {
		WebImage(
			uri = info.avatarPath,
			key = remember { DateEx.TodayString },
			contentScale = ContentScale.Crop,
			circle = true,
			modifier = Modifier.size(ThemeValue.Size.MicroImage)
		)
		Text(
			text = info.name,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.weight(1f)
		)
	}
}

@Stable
class ScreenTopic(model: AppModel, args: Args) : SubScreen<ScreenTopic.Args>(model) {
	@Stable
	@Serializable
	data class Args(val currentTopic: Topic)

	private var details: TopicDetails? by mutableStateOf(null)
	private var topic: Topic by mutableStateOf(args.currentTopic)

	private val pageComments = object : PaginationArgs<Comment, Int, Int, Boolean>(0, true) {
		override fun distinctValue(item: Comment): Int = item.cid
		override fun offset(item: Comment): Int = item.cid
		override fun arg1(item: Comment): Boolean = item.isTop
	}

	private val listState = LazyListState()

	private var currentSendComment: Comment? by mutableStateOf(null)

	private val sendCommentState = object : RichEditorState() {
		override val useImage: Boolean = true
		override val useAt: Boolean = true

		@Composable
		override fun AtLayout(modifier: Modifier) {
			val userList = remember(details, pageComments.items) {
				val userSet = mutableSetOf(AtInfo(topic.uid, topic.name))
				pageComments.items.fastForEach { userSet.add(AtInfo(it.uid, it.name)) }
				userSet -= AtInfo(app.config.userProfile?.uid ?: 0, "")
				userSet.toList()
			}

			LazyVerticalGrid(
				columns = GridCells.Adaptive(ThemeValue.Size.CellWidth),
				modifier = modifier
			) {
				items(
					items = userList,
					key = { it.uid }
				) {
					AtUserItem(
						info = it,
						onClick = { closeLayout("[at|${it.uid}|@${it.name}]") },
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}

		@Composable
		override fun ImageLayout(modifier: Modifier) {
			UnsupportedComponent(modifier = modifier)
		}
	}

	private suspend fun requestTopic() {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicDetails,
			data = topic.tid
		)
		if (result is Data.Success) details = result.data
	}

	private suspend fun requestNewComments() {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicComments,
			data = API.User.Topic.GetTopicComments.Request(
				tid = topic.tid,
				rawSection = topic.rawSection,
				num = pageComments.pageNum
			)
		)
		if (result is Data.Success) pageComments.newData(result.data)
	}

	private suspend fun requestMoreComments() {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicComments,
			data = API.User.Topic.GetTopicComments.Request(
				tid = topic.tid,
				rawSection = topic.rawSection,
				cid = pageComments.offset,
				isTop = pageComments.arg1
			)
		)
		if (result is Data.Success) pageComments.moreData(result.data)
	}

	private suspend fun requestSubComments(pid: Int, cid: Int, num: Int): List<SubComment>? {
		val result = ClientAPI.request(
			route = API.User.Topic.GetTopicSubComments,
			data = API.User.Topic.GetTopicSubComments.Request(
				pid = pid,
				rawSection = topic.rawSection,
				cid = cid,
				num = num
			)
		)
		return if (result is Data.Success) result.data else null
	}

	private fun onAvatarClick(uid: Int) {
		discoveryPart.onUserAvatarClick(uid)
	}

	private fun onImageClick(images: List<Picture>, current: Int) {
		navigate(ScreenImagePreview.Args(images, current))
	}

	private suspend fun onChangeTopicIsTop(value: Boolean) {
		val result = ClientAPI.request(
			route = API.User.Topic.UpdateTopicTop,
			data = API.User.Topic.UpdateTopicTop.Request(
				token = app.config.userToken,
				tid = topic.tid,
				isTop = value
			)
		)
		when (result) {
			is Data.Success -> topic = topic.copy(isTop = value)
			is Data.Error -> slot.tip.error(result.message)
		}
	}

	private suspend fun onDeleteTopic() {
		if (slot.confirm.openSuspend(content = "删除主题?")) {
			val result = ClientAPI.request(
				route = API.User.Topic.DeleteTopic,
				data =  API.User.Topic.DeleteTopic.Request(
					token = app.config.userToken,
					tid = topic.tid
				)
			)
			when (result) {
				is Data.Success -> {
					discoveryPart.page.items.removeAll { it.tid == topic.tid }
					pop()
				}
				is Data.Error -> slot.tip.error(result.message)
			}
		}
	}

	private suspend fun onMoveTopic() {
		moveTopicDialog.openSuspend()?.let { index ->
			val newSection = Comment.Section.MovableSection[index]
			details?.let { oldDetails ->
				val oldSection = oldDetails.section
				if (newSection == oldSection) {
					slot.tip.warning("不能与原板块相同哦")
					return@let
				}
				val result = ClientAPI.request(
					route = API.User.Topic.MoveTopic,
					data = API.User.Topic.MoveTopic.Request(
						token = app.config.userToken,
						tid = topic.tid,
						section = newSection
					)
				)
				when (result) {
					is Data.Success -> {
						if (discoveryPart.currentSection == oldSection) discoveryPart.page.items.removeAll { it.tid == topic.tid }
						details = oldDetails.copy(section = newSection)
						slot.tip.success(result.message)
					}
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}
	}

	private suspend fun onSendCoin(num: Int) {
		if (app.config.userProfile?.uid == topic.uid) {
			slot.tip.warning("不能给自己投币哦")
			return
		}
		val result = ClientAPI.request(
			route = API.User.Topic.SendCoin,
			data = API.User.Topic.SendCoin.Request(
				token = app.config.userToken,
				uid = topic.uid,
				tid = topic.tid,
				value = num
			)
		)
		when (result) {
			is Data.Success -> {
				discoveryPart.page.items.findAssign(predicate = { it.tid == topic.tid }) {
					it.copy(coinNum = it.coinNum + num)
				}
				app.config.userProfile?.let {
					app.config.userProfile = it.copy(coin = it.coin - num)
				}
				slot.tip.success(result.message)
			}
			is Data.Error -> slot.tip.error(result.message)
		}
	}

	private suspend fun onSendComment(content: String): Boolean {
		app.config.userProfile?.let { user ->
			// 回复主题
			val target = currentSendComment
			if (target == null) {
				val result = ClientAPI.request(
					route = API.User.Topic.SendComment,
					data = API.User.Topic.SendComment.Request(
						token = app.config.userToken,
						tid = topic.tid,
						rawSection = topic.rawSection,
						content = content
					)
				)
				when (result) {
					is Data.Success -> {
						discoveryPart.page.items.findAssign(predicate = { it.tid == topic.tid }) {
							it.copy(commentNum = it.commentNum + 1)
						}
						pageComments.items += Comment(
							cid = result.data,
							uid = user.uid,
							ts = DateEx.CurrentString,
							content = content,
							isTop = false,
							subCommentNum = 0,
							name = user.name,
							label = user.label,
							exp = user.exp
						)
						listState.animateScrollToItem(pageComments.items.size - 1)
						return true
					}
					is Data.Error -> slot.tip.error(result.message)
				}
			}
			else { // 回复评论
				val result = ClientAPI.request(
					route = API.User.Topic.SendSubComment,
					data = API.User.Topic.SendSubComment.Request(
						token = app.config.userToken,
						tid = topic.tid,
						cid = target.cid,
						rawSection = topic.rawSection,
						content = content
					)
				)
				when (result) {
					is Data.Success -> {
						pageComments.items.findAssign(predicate = { it.cid == target.cid }) {
							it.copy(subCommentNum = it.subCommentNum + 1)
						}
						currentSendComment = null
						return true
					}
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}
		return false
	}

	private suspend fun onChangeCommentIsTop(cid: Int, isTop: Boolean) {
		val result = ClientAPI.request(
			route = API.User.Topic.UpdateCommentTop,
			data = API.User.Topic.UpdateCommentTop.Request(
				token = app.config.userToken,
				tid = topic.tid,
				cid = cid,
				rawSection = topic.rawSection,
				isTop = isTop
			)
		)
		when (result) {
			is Data.Success -> {
				pageComments.items.findAssign(predicate = { it.cid == cid }) {
					it.copy(isTop = isTop)
				}
				pageComments.items.sort()
				listState.scrollToItem(pageComments.items.indexOfFirst { it.cid == cid })
			}
			is Data.Error -> slot.tip.error(result.message)
		}
	}

	private suspend fun onDeleteComment(cid: Int) {
		if (slot.confirm.openSuspend(content = "删除回复(楼中楼会同步删除)")) {
			val result = ClientAPI.request(
				route = API.User.Topic.DeleteComment,
				data = API.User.Topic.DeleteComment.Request(
					token = app.config.userToken,
					tid = topic.tid,
					cid = cid,
					rawSection = topic.rawSection
				)
			)
			when (result) {
				is Data.Success -> {
					discoveryPart.page.items.findAssign(predicate = { it.tid == topic.tid }) {
						it.copy(commentNum = it.commentNum - 1)
					}
					pageComments.items.removeAll { it.cid == cid }
				}
				is Data.Error -> slot.tip.error(result.message)
			}
		}
	}

	private suspend fun onDeleteSubComment(pid: Int, cid: Int, onDelete: () -> Unit) {
		if (slot.confirm.openSuspend(content = "删除回复")) {
			val result = ClientAPI.request(
				route = API.User.Topic.DeleteSubComment,
				data = API.User.Topic.DeleteSubComment.Request(
					token = app.config.userToken,
					tid = topic.tid,
					pid = pid,
					rawSection = topic.rawSection,
					cid = cid
				)
			)
			when (result) {
				is Data.Success -> onDelete()
				is Data.Error -> slot.tip.error(result.message)
			}
		}
	}

	@Composable
	private fun RichTextLayout(
		text: RichString,
		modifier: Modifier = Modifier
	) {
		RichText(
			text = text,
			fixLineHeight = true,
			onLinkClick = { gotoWebPage(it) },
			onTopicClick = {},
			onAtClick = { navigate(ScreenUserCard.Args(it.toIntOrNull() ?: 0)) },
			modifier = modifier
		)
	}

	@Composable
	private fun TopicLayout(details: TopicDetails, modifier: Modifier = Modifier) {
		val pics = remember(details, topic) { details.pics.fastMap { Picture(topic.picPath(it)) } }

		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
		) {
			UserBar(
				avatar = topic.avatarPath,
				name = topic.name,
				time = details.ts,
				label = details.label,
				level = details.level,
				onAvatarClick = { onAvatarClick(topic.uid) }
			)
			SelectionContainer {
				Text(
					text = topic.title,
					style = MaterialTheme.typography.titleMedium,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.fillMaxWidth()
				)
			}
			RichTextLayout(
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
		}
	}

	@Composable
	private fun CommentBar(comment: Comment, modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				if (comment.isTop) BoxText(text = "置顶", color = MaterialTheme.colorScheme.primary)
				if (comment.uid == topic.uid) BoxText(text = "楼主", color = MaterialTheme.colorScheme.secondary)
			}
			RichTextLayout(
				text = remember(comment) { RichString.parse(comment.content) },
				modifier = Modifier.fillMaxWidth()
			)
			Row(
				modifier = Modifier.fillMaxWidth().padding(top = ThemeValue.Padding.VerticalSpace),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.End),
				verticalAlignment = Alignment.CenterVertically
			) {
				if (comment.subCommentNum > 0) {
					Box(modifier = Modifier.weight(1f)) {
						Text(
							text = ">> 查看${comment.subCommentNum}条回复",
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.primary,
							modifier = Modifier.clickable { subCommentSheet.open(comment) }
								.padding(ThemeValue.Padding.LittleValue)
						)
					}
				}
				Text(
					text = "回复",
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.secondary,
					modifier = Modifier.clickable { currentSendComment = comment }
						.padding(ThemeValue.Padding.LittleValue)
				)
				app.config.userProfile?.let { user ->
					if (user.canUpdateCommentTop(topic.uid)) {
						Text(
							text = if (comment.isTop) "取消置顶" else "置顶",
							style = MaterialTheme.typography.labelMedium,
							modifier = Modifier.clickable {
								launch { onChangeCommentIsTop(comment.cid, !comment.isTop) }
							}.padding(ThemeValue.Padding.LittleValue)
						)
					}
					if (user.canDeleteComment(topic.uid, comment.uid)) {
						Text(
							text = "删除",
							style = MaterialTheme.typography.labelMedium,
							modifier = Modifier.clickable {
								launch { onDeleteComment(comment.cid) }
							}.padding(ThemeValue.Padding.LittleValue)
						)
					}
				}
			}
		}
	}

	@Composable
	private fun SubCommentBar(
		subComment: SubComment,
		parentComment: Comment,
		modifier: Modifier = Modifier,
		onDelete: () -> Unit
	) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			UserBar(
				avatar = subComment.avatarPath,
				name = subComment.name,
				time = subComment.ts,
				label = subComment.label,
				level = subComment.level,
				onAvatarClick = {
					subCommentSheet.close()
					onAvatarClick(subComment.uid)
				}
			)
			Row(
				modifier = Modifier.fillMaxWidth().padding(top = ThemeValue.Padding.VerticalSpace),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				if (subComment.uid == topic.uid) BoxText(text = "楼主", color = MaterialTheme.colorScheme.secondary)
				if (subComment.uid == parentComment.uid) BoxText(text = "层主", color = MaterialTheme.colorScheme.tertiary)
				app.config.userProfile?.let { user ->
					if (user.canDeleteComment(topic.uid, subComment.uid)) {
						Row(
							modifier = Modifier.weight(1f),
							horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.End),
							verticalAlignment = Alignment.CenterVertically
						) {
							Text(
								text = "删除",
								textAlign = TextAlign.End,
								style = MaterialTheme.typography.labelMedium,
								modifier = Modifier.clickable {
									launch { onDeleteSubComment(parentComment.cid, subComment.cid, onDelete) }
								}.padding(ThemeValue.Padding.LittleValue)
							)
						}
					}
				}
			}
			RichTextLayout(
				text = remember(subComment) { RichString.parse(subComment.content) },
				modifier = Modifier.fillMaxWidth()
			)
		}
	}

	@Composable
	private fun BottomLayout(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			SplitActionLayout(
				modifier = Modifier.fillMaxWidth(),
				left = {
					Action(
						icon = Icons.Filled.Home,
                        tip = "回复楼主",
						enabled = currentSendComment != null
					) {
						currentSendComment = null
					}
				},
				right = {
					Action(Icons.Filled.Paid, "投币") {
						sendCoinSheet.open()
					}
					ActionSuspend(
						icon = Icons.AutoMirrored.Filled.Send,
                        tip = "发送",
						enabled = sendCommentState.ok
					) {
						if (onSendComment(sendCommentState.richString.toString())) {
							sendCommentState.text = ""
							sendCommentState.closePreview()
						}
					}
				}
			)
			RichEditor(
				state = sendCommentState,
				hint = remember(currentSendComment) { "回复 @${currentSendComment?.name ?: "主题"}" },
				maxLength = 256,
				modifier = Modifier.fillMaxWidth(),
			)
		}
	}

	@Composable
	private fun Portrait(details: TopicDetails) {
		bindPauseLoadWhenScrolling(listState)

		PaginationColumn(
			items = pageComments.items,
			key = { it.cid },
			state = listState,
			canRefresh = false,
			canLoading = pageComments.canLoading,
			onLoading = { requestMoreComments() },
			modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
			header = {
				Surface(
					modifier = Modifier.fillMaxWidth(),
					tonalElevation = ThemeValue.Shadow.Tonal
				) {
					TopicLayout(
						details = details,
						modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue)
					)
				}
				HorizontalDivider(modifier = Modifier.padding(bottom = ThemeValue.Padding.VerticalSpace))
			},
			itemDivider = PaddingValues(vertical = ThemeValue.Padding.VerticalSpace)
		) {
			CommentBar(
				comment = it,
				modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalSpace)
			)
		}
	}

	@Composable
	private fun Landscape(details: TopicDetails) {
		Row(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier
					.padding(LocalImmersivePadding.current.withoutEnd)
					.width(ThemeValue.Size.PanelWidth)
					.fillMaxHeight()
					.verticalScroll(rememberScrollState()),
				tonalElevation = ThemeValue.Shadow.Tonal
			) {
				TopicLayout(
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue),
					details = details
				)
			}
			VerticalDivider()

			bindPauseLoadWhenScrolling(listState)

			PaginationColumn(
				items = pageComments.items,
				key = { it.cid },
				state = listState,
				canRefresh = false,
				canLoading = pageComments.canLoading,
				onLoading = { requestMoreComments() },
				itemDivider = PaddingValues(vertical = ThemeValue.Padding.VerticalSpace),
				modifier = Modifier
					.padding(LocalImmersivePadding.current.withoutStart)
					.weight(1f)
					.fillMaxHeight()
					.padding(ThemeValue.Padding.Value)
			) {
				CommentBar(
					comment = it,
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}

	override val title: String = "主题"

	override suspend fun initialize() {
		requestTopic()
		requestNewComments()
	}

	override fun onBack() {
		if (subCommentSheet.isOpen) subCommentSheet.close()
		else pop()
	}

	@Composable
	override fun ActionScope.RightActions() {
		if (details != null) {
			val canUpdateTopicTop by rememberDerivedState { app.config.userProfile?.canUpdateTopicTop(topic.uid) == true }
			val canDeleteTopic by rememberDerivedState { app.config.userProfile?.canDeleteTopic(topic.uid) == true }
			val canMoveTopic by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPTopic == true }
			if (canUpdateTopicTop) {
				ActionSuspend(
                    icon = if (topic.isTop) Icons.Outlined.MobiledataOff else Icons.Outlined.VerticalAlignTop,
                    tip = if (topic.isTop) "取消置顶" else "置顶"
                ) {
					onChangeTopicIsTop(!topic.isTop)
				}
			}
			if (canMoveTopic) {
				ActionSuspend(Icons.Outlined.MoveUp, "移动") {
					onMoveTopic()
				}
			}
			if (canDeleteTopic) {
				ActionSuspend(Icons.Outlined.Delete, "删除") {
					onDeleteTopic()
				}
			}
		}
	}

	@Composable
	override fun BottomBar() {
		if (details != null && app.config.userProfile != null) {
			BottomLayout(modifier = Modifier
				.padding(LocalImmersivePadding.current)
				.fillMaxWidth()
				.padding(ThemeValue.Padding.EqualValue)
			)
		}
	}

	@Composable
	override fun SubContent(device: Device) {
		details?.let {
			when (device.type) {
				Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(details = it)
				Device.Type.LANDSCAPE -> Landscape(details = it)
			}
		} ?: EmptyBox()
	}

	private val subCommentSheet = object : FloatingArgsSheet<Comment>() {
		var page: Pagination<SubComment, Int, Int> by mutableStateOf(object : Pagination<SubComment, Int, Int>(0) {
			override fun distinctValue(item: SubComment): Int = item.cid
			override fun offset(item: SubComment): Int = item.cid
		})

		override suspend fun initialize(args: Comment) {
			page = object : Pagination<SubComment, Int, Int>(0) {
				override fun distinctValue(item: SubComment): Int = item.cid
				override fun offset(item: SubComment): Int = item.cid
			}
			requestSubComments(
				pid = args.cid,
				cid = page.offset,
				num = page.pageNum
			)?.let { page.newData(it) }
		}

		@Composable
		override fun Content(args: Comment) {
			val state = rememberLazyListState()
			bindPauseLoadWhenScrolling(state)

			PaginationColumn(
				items = page.items,
				key = { it.cid },
				state = state,
				canRefresh = false,
				canLoading = page.canLoading,
				onLoading = {
					requestSubComments(
						pid = args.cid,
						cid = page.offset,
						num = page.pageNum
					)?.let { page.moreData(it) }
				},
				itemDivider = PaddingValues(vertical = ThemeValue.Padding.VerticalExtraSpace),
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue)
			) { subComment ->
				SubCommentBar(
					subComment = subComment,
					parentComment = args,
					modifier = Modifier.fillMaxWidth(),
					onDelete = {
						page.items -= subComment
						pageComments.items.findAssign(predicate = { it.cid == args.cid }) {
							it.copy(subCommentNum = it.subCommentNum - 1)
						}
						// 楼中楼最后一条回复删除后隐藏楼中楼
						if (page.items.isEmpty()) close()
					}
				)
			}
		}
	}

	private val sendCoinSheet = object : FloatingSheet() {
		@Composable
		override fun Content() {
			Column(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
			) {
				Text(
					text = "银币: ${app.config.userProfile?.coin ?: 0}",
					style = MaterialTheme.typography.titleLarge,
					textAlign = TextAlign.Center,
					modifier = Modifier.fillMaxWidth()
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
					verticalAlignment = Alignment.CenterVertically
				) {
					repeat(3) {
						CoinLayout(
							num = it + 1,
							modifier = Modifier.weight(1f).aspectRatio(1f),
							onClick = { num ->
								close()
								launch { onSendCoin(num) }
							}
						)
					}
				}
			}
		}
	}

	private val moveTopicDialog = FloatingDialogChoice.fromItems(
		items = Comment.Section.MovableSection.fastMap { Comment.Section.sectionName(it) },
		title = "移动主题板块"
	)

	@Composable
	override fun Floating() {
		subCommentSheet.Land()
		sendCoinSheet.Land()
		moveTopicDialog.Land()
	}
}