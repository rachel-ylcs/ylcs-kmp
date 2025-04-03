package love.yinlin.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.ThemeColor
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
import love.yinlin.platform.app
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.screen.*
import love.yinlin.ui.component.screen.SheetState
import love.yinlin.ui.component.text.RichString
import love.yinlin.ui.component.text.RichText
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.Screen
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

@Composable
private fun CoinLayout(
	num: Int,
	modifier: Modifier = Modifier,
	onClick: (Int) -> Unit
) {
	Surface(
		modifier = modifier,
		shape = MaterialTheme.shapes.large,
		tonalElevation = 3.dp,
		shadowElevation = 3.dp
	) {
		Column(
			modifier = Modifier.clickable{ onClick(num) }.padding(10.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			Row (
				modifier = Modifier.fillMaxWidth().weight(1f),
				horizontalArrangement = Arrangement.Center
			) {
				repeat(num) {
					MiniIcon(
						imageVector = Icons.Filled.Paid,
						color = when (num) {
							1 -> MaterialTheme.colorScheme.tertiary
							2 -> MaterialTheme.colorScheme.secondary
							else -> MaterialTheme.colorScheme.primary
						},
						modifier = Modifier.size(32.dp - 1.5.dp * num)
					)
				}
			}
			if (num == UserConstraint.MIN_COIN_REWARD) Text(text = "作者获赠1银币", color = ThemeColor.fade, style = MaterialTheme.typography.bodyMedium)
			Text(text = "$num 银币", style = MaterialTheme.typography.bodyLarge)
		}
	}
}

@Stable
@Serializable
data class ScreenTopic(val currentTopic: Topic) : Screen<ScreenTopic.Model> {
	inner class Model(model: AppModel) : Screen.Model(model) {
		var details: TopicDetails? by mutableStateOf(null)
		var topic: Topic by mutableStateOf(currentTopic)

		val commentPage = object : PaginationArgs<Comment, Int, Boolean>(0, true) {
			override fun offset(item: Comment): Int = item.cid
			override fun arg1(item: Comment): Boolean = item.isTop
		}
		val commentState = LazyListState()

		var currentSendComment: Comment? by mutableStateOf(null)

		val subCommentSheet = SheetState<Comment>()
		val sendCoinSheet = CommonSheetState()

		val moveTopicDialog = DialogChoice.fromItems(
			items = Comment.Section.MovableSection.map { Comment.Section.sectionName(it) },
			title = "移动主题板块"
		) { index, _ ->
			onMoveTopic(Comment.Section.MovableSection[index])
		}

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
					rawSection = topic.rawSection,
					num = commentPage.pageNum
				)
			)
			if (result is Data.Success) commentPage.newData(result.data)
		}

		suspend fun requestMoreComments() {
			val result = ClientAPI.request(
				route = API.User.Topic.GetTopicComments,
				data = API.User.Topic.GetTopicComments.Request(
					tid = topic.tid,
					rawSection = topic.rawSection,
					offset = commentPage.offset,
					isTop = commentPage.arg1
				)
			)
			if (result is Data.Success) commentPage.moreData(result.data)
		}

		suspend fun requestSubComments(cid: Int, offset: Int, num: Int): List<SubComment>? {
			val result = ClientAPI.request(
				route = API.User.Topic.GetTopicSubComments,
				data = API.User.Topic.GetTopicSubComments.Request(
					cid = cid,
					rawSection = topic.rawSection,
					offset = offset,
					num = num
				)
			)
			return if (result is Data.Success) result.data else null
		}

		fun onAvatarClick(uid: Int) {
			part<ScreenPartDiscovery>().onUserAvatarClick(uid)
		}

		fun onImageClick(images: List<Picture>, current: Int) {
			navigate(ScreenImagePreview(images, current))
		}

		fun onChangeTopicIsTop(value: Boolean) {
			launch {
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
		}

		fun onDeleteTopic() {
			launch {
				val result = ClientAPI.request(
					route = API.User.Topic.DeleteTopic,
					data =  API.User.Topic.DeleteTopic.Request(
						token = app.config.userToken,
						tid = topic.tid
					)
				)
				when (result) {
					is Data.Success -> {
						part<ScreenPartDiscovery>().page.items.removeAll { it.tid == topic.tid }
						pop()
					}
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}

		fun onMoveTopic(newSection: Int) {
			details?.let { oldDetails ->
				val oldSection = oldDetails.section
				launch {
					if (newSection == oldSection) {
						slot.tip.warning("不能与原板块相同哦")
						return@launch
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
							val part = part<ScreenPartDiscovery>()
							if (part.currentSection == oldSection) part.page.items.removeAll { it.tid == topic.tid }
							details = oldDetails.copy(section = newSection)
							slot.tip.success(result.message)
						}
						is Data.Error -> slot.tip.error(result.message)
					}
				}
			}
		}

		fun onSendCoin(num: Int) {
			launch {
				if (app.config.userProfile?.uid == topic.uid) {
					slot.tip.warning("不能给自己投币哦")
					return@launch
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
						part<ScreenPartDiscovery>().page.items.findAssign(predicate = { it.tid == topic.tid }) {
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
		}

		suspend fun onSendComment(content: String): Boolean {
			val user = app.config.userProfile
			if (user != null) {
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
							part<ScreenPartDiscovery>().page.items.findAssign(predicate = { it.tid == topic.tid }) {
								it.copy(commentNum = it.commentNum + 1)
							}
							commentPage.items += Comment(
                                cid = result.data,
                                uid = user.uid,
                                ts = DateEx.CurrentString,
                                content = content,
                                isTop = false,
                                subCommentNum = 0,
                                name = user.name,
                                label = user.label,
                                coin = user.coin
                            )
							commentState.animateScrollToItem(commentPage.items.size - 1)
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
							commentPage.items.findAssign(predicate = { it.cid == target.cid }) {
								it.copy(subCommentNum = it.subCommentNum + 1)
							}
							currentSendComment = null
							return true
						}
						is Data.Error -> slot.tip.error(result.message)
					}
				}
			}
			else slot.tip.error("请先登录")
			return false
		}

		fun onChangeCommentIsTop(cid: Int, isTop: Boolean) {
			launch {
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
						commentPage.items.findAssign(predicate = { it.cid == cid }) {
							it.copy(isTop = isTop)
						}
						commentPage.items.sort()
						commentState.scrollToItem(commentPage.items.indexOfFirst { it.cid == cid })
					}
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}

		fun onDeleteComment(cid: Int) {
			launch {
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
						part<ScreenPartDiscovery>().page.items.findAssign(predicate = { it.tid == topic.tid }) {
							it.copy(commentNum = it.commentNum - 1)
						}
						commentPage.items.removeAll { it.cid == cid }
					}
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}

		fun onDeleteSubComment(pid: Int, cid: Int, onDelete: () -> Unit) {
			launch {
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
								modifier = Modifier.clickable { subCommentSheet.open(comment) }.padding(2.dp)
							)
						}
					}
					Text(
						text = "回复",
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.secondary,
						modifier = Modifier.clickable { currentSendComment = comment }.padding(2.dp)
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
								modifier = Modifier.clickable {
									slot.confirm.open(content = "删除回复(楼中楼会同步删除)") {
										onDeleteComment(comment.cid)
									}
								}.padding(2.dp)
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
				verticalArrangement = Arrangement.spacedBy(3.dp)
			) {
				UserBar(
					avatar = subComment.avatarPath,
					name = subComment.name,
					time = subComment.ts,
					label = subComment.label,
					level = subComment.level,
					onAvatarClick = {
						subCommentSheet.hide()
						onAvatarClick(subComment.uid)
					}
				)
				Row(
					modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
					horizontalArrangement = Arrangement.spacedBy(5.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					if (subComment.uid == topic.uid) BoxText(text = "楼主", color = MaterialTheme.colorScheme.secondary)
					if (subComment.uid == parentComment.uid) BoxText(text = "层主", color = MaterialTheme.colorScheme.tertiary)
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
									modifier = Modifier.clickable {
										slot.confirm.open(content = "删除回复") {
											onDeleteSubComment(parentComment.cid, subComment.cid, onDelete)
										}
									}.padding(2.dp)
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

		@Composable
		fun SubCommentLayout(comment: Comment) {
			val page = remember(comment) { object : Pagination<SubComment, Int>(0) {
				override fun offset(item: SubComment): Int = item.cid
			} }

			BottomSheet(state = subCommentSheet) {
				PaginationColumn(
					items = page.items,
					key = { it.cid },
					canRefresh = false,
					canLoading = page.canLoading,
					onLoading = {
						requestSubComments(
							cid = comment.cid,
							offset = page.offset,
							num = page.pageNum
						)?.let { page.moreData(it) }
					},
					contentPadding = PaddingValues(vertical = 10.dp),
					itemDivider = PaddingValues(vertical = 8.dp),
					modifier = Modifier.fillMaxWidth()
				) { subComment ->
					SubCommentBar(
						subComment = subComment,
						parentComment = comment,
						modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
						onDelete = {
							page.items -= subComment
							commentPage.items.findAssign(predicate = { it.cid == comment.cid }) {
								it.copy(subCommentNum = it.subCommentNum - 1)
							}
							// 楼中楼最后一条回复删除后隐藏楼中楼
							if (page.items.isEmpty()) subCommentSheet.hide()
						}
					)
				}
			}

			LaunchedEffect(Unit) {
				requestSubComments(
					cid = comment.cid,
					offset = page.offset,
					num = page.pageNum
				)?.let { page.newData(it) }
			}
		}

		@Composable
		fun SendCoinLayout() {
			BottomSheet(state = sendCoinSheet) {
				Column(
					modifier = Modifier.fillMaxWidth().padding(10.dp),
					verticalArrangement = Arrangement.spacedBy(10.dp)
				) {
					Text(
						text = "银币: ${app.config.userProfile?.coin ?: 0}",
						style = MaterialTheme.typography.titleLarge,
						textAlign = TextAlign.Center,
						modifier = Modifier.fillMaxWidth()
					)
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						repeat(3) {
							CoinLayout(
								num = it + 1,
								modifier = Modifier.weight(1f).aspectRatio(1f),
								onClick = { num ->
									sendCoinSheet.hide()
									onSendCoin(num)
								}
							)
						}
					}
				}
			}
		}

		@Composable
		fun BottomLayout(modifier: Modifier = Modifier) {
			val state = remember(currentSendComment) { TextInputState() }

			Column(
				modifier = modifier,
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(5.dp)
			) {
				SplitActionLayout(
					modifier = Modifier.fillMaxWidth(),
					left = {
						Action(
							icon = Icons.Filled.Home,
							enabled = currentSendComment != null,
							onClick = { currentSendComment = null }
						)
					},
					right = {
						Action(
							icon = Icons.Filled.Paid,
							onClick = { sendCoinSheet.open() }
						)
						ActionSuspend(
							icon = Icons.AutoMirrored.Filled.Send,
							enabled = state.text.isNotEmpty(),
							onClick = {
								if (onSendComment(state.text)) {
									state.text = ""
								}
							}
						)
					}
				)
				TextInput(
					state = state,
					hint = remember(currentSendComment) { "回复 @${currentSendComment?.name ?: "主题"}" },
					maxLines = 5,
					maxLength = 1024,
					clearButton = false,
					modifier = Modifier.fillMaxWidth()
				)
			}
		}

		@Composable
		fun Portrait(details: TopicDetails) {
			PaginationColumn(
				items = commentPage.items,
				key = { it.cid },
				state = commentState,
				canRefresh = false,
				canLoading = commentPage.canLoading,
				onLoading = { requestMoreComments() },
				modifier = Modifier.fillMaxSize(),
				header = {
					Surface(
						modifier = Modifier.fillMaxWidth(),
						tonalElevation = 1.dp
					) {
						TopicLayout(
							details = details,
							modifier = Modifier.fillMaxWidth().padding(10.dp)
						)
					}
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
				Surface(
					modifier = Modifier.width(400.dp).fillMaxHeight()
						.verticalScroll(rememberScrollState()),
					tonalElevation = 1.dp
				) {
					TopicLayout(
						modifier = Modifier.fillMaxWidth().padding(10.dp),
						details = details
					)
				}
				VerticalDivider()
				PaginationColumn(
					items = commentPage.items,
					key = { it.cid },
					state = commentState,
					canRefresh = false,
					canLoading = commentPage.canLoading,
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
			actions = {
				if (model.details != null) {
					val canUpdateTopicTop by rememberDerivedState { app.config.userProfile?.canUpdateTopicTop(model.topic.uid) == true }
					val canDeleteTopic by rememberDerivedState { app.config.userProfile?.canDeleteTopic(model.topic.uid) == true }
					val canMoveTopic by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPTopic == true }
					if (canUpdateTopicTop) Action(
						icon = if (model.topic.isTop) Icons.Outlined.MobiledataOff else Icons.Outlined.VerticalAlignTop,
						onClick = { model.onChangeTopicIsTop(!model.topic.isTop) }
					)
					if (canMoveTopic) Action(
						icon = Icons.Outlined.MoveUp,
						onClick = { model.moveTopicDialog.open() }
					)
					if (canDeleteTopic) Action(
						icon = Icons.Outlined.Delete,
						onClick = {
							model.slot.confirm.open(content = "删除主题?") {
								model.onDeleteTopic()
							}
						}
					)
				}
			},
			bottomBar = {
				if (model.details != null) {
					model.BottomLayout(modifier = Modifier.fillMaxWidth().padding(10.dp))
				}
			},
			onBack = {
				if (model.subCommentSheet.isOpen) model.subCommentSheet.hide()
				else model.pop()
			},
			slot = model.slot
		) {
			val details = model.details
			if (details == null) EmptyBox()
			else if (app.isPortrait) model.Portrait(details = details)
			else model.Landscape(details = details)
		}

		model.subCommentSheet.withOpen {
			model.SubCommentLayout(it)
		}

		model.sendCoinSheet.withOpen {
			model.SendCoinLayout()
		}

		model.moveTopicDialog.withOpen()
	}
}