package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.api.WeiboAPI
import love.yinlin.app
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.layout.NineGrid
import love.yinlin.ui.component.text.RichText
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.LaunchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.AppModel
import love.yinlin.launch
import love.yinlin.platform.Coroutines
import love.yinlin.ui.common.WeiboLayout
import love.yinlin.ui.common.WeiboDataBar
import love.yinlin.ui.common.WeiboUserBar

class WeiboDetailsModel(model: AppModel) : ViewModel() {
	val msgModel = model.mainModel.msgModel
	val weibo: Weibo? = msgModel.currentWeibo
	val launchFlag = LaunchFlag()
	var comments: List<WeiboComment>? by mutableStateOf(null)

	fun init(weibo: Weibo) {
		launch {
			comments = Coroutines.io {
				val data = WeiboAPI.getWeiboDetails(weibo.id)
				if (data is Data.Success) data.data else emptyList()
			}
		}
	}
}

@Composable
private fun WeiboCommentCard(
	onAvatarClick: (WeiboUserInfo) -> Unit,
	comment: WeiboComment
) {
	Column(
		modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
	) {
		WeiboUserBar(
			avatar = comment.info.avatar,
			name = comment.info.name,
			location = comment.location,
			time = comment.time,
			modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
			onAvatarClick = { onAvatarClick(comment.info) }
		)
		RichText(
			text = comment.text,
			modifier = Modifier.fillMaxWidth()
		)
		val subComments = comment.subComments
		if (subComments.isNotEmpty()) {
			Surface(
				modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 10.dp),
				tonalElevation = 3.dp
			) {
				Column(
					modifier = Modifier.fillMaxWidth().padding(5.dp)
				) {
					for (subComment in subComments) {
						WeiboUserBar(
							avatar = subComment.info.avatar,
							name = subComment.info.name,
							location = subComment.location,
							time = subComment.time,
							modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
							onAvatarClick = { onAvatarClick(subComment.info) }
						)
						RichText(
							text = subComment.text,
							modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)
						)
					}
				}
			}
		}
	}
}

@Composable
private fun Portrait(
	model: WeiboDetailsModel,
	weibo: Weibo,
	comments: List<WeiboComment>?
) {
	LazyColumn(
		modifier = Modifier.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface)
			.padding(start = 10.dp, end = 10.dp, top = 5.dp)
	) {
		item(key = -1) {
			WeiboLayout(
				weibo = weibo,
				onAvatarClick = { model.msgModel.onWeiboAvatarClick(it) },
				onLinkClick = { model.msgModel.onWeiboLinkClick(it) },
				onTopicClick = { model.msgModel.onWeiboTopicClick(it) },
				onAtClick = { model.msgModel.onWeiboAtClick(it) }
			)
		}
		if (comments != null) {
			item(key = -2) {
				HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
			}
			items(
				items = comments,
				key = { it.id }
			) {
				WeiboCommentCard(
					onAvatarClick = { info -> model.msgModel.onWeiboAvatarClick(info) },
					comment = it
				)
			}
		}
	}
}

@Composable
private fun Landscape(
	model: WeiboDetailsModel,
	weibo: Weibo,
	comments: List<WeiboComment>?
) {
	Row(
		modifier = Modifier.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface)
			.padding(10.dp)
	) {
		Column(modifier = Modifier.width(360.dp).fillMaxHeight()) {
			WeiboUserBar(
				modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
				avatar = weibo.info.avatar,
				name = weibo.info.name,
				time = weibo.time,
				location = weibo.location,
				onAvatarClick = { model.msgModel.onWeiboAvatarClick(weibo.info) }
			)
			RichText(
				text = weibo.text,
				modifier = Modifier.fillMaxWidth().weight(1f),
				overflow = TextOverflow.Ellipsis,
				onLinkClick = { model.msgModel.onWeiboLinkClick(it) },
				onTopicClick = { model.msgModel.onWeiboTopicClick(it) },
				onAtClick = { model.msgModel.onWeiboAtClick(it) }
			)
		}
		VerticalDivider(modifier = Modifier.padding(horizontal = 10.dp))
		Column(modifier = Modifier.width(360.dp).fillMaxHeight()) {
			WeiboDataBar(
				like = weibo.likeNum,
				comment = weibo.commentNum,
				repost = weibo.repostNum,
				modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
			)
			if (weibo.pictures.isNotEmpty()) {
				NineGrid(
					pics = weibo.pictures,
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
		VerticalDivider(modifier = Modifier.padding(horizontal = 10.dp))
		Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
			if (comments == null) LoadingBox()
			else if (comments.isEmpty()) EmptyBox()
			else LazyColumn(modifier = Modifier.fillMaxSize()) {
				items(
					items = comments,
					key = { it.id }
				) {
					WeiboCommentCard(
						onAvatarClick = { info -> model.msgModel.onWeiboAvatarClick(info) },
						comment = it
					)
				}
			}
		}
	}
}

@Composable
fun ScreenWeiboDetails(model: AppModel) {
	val screenModel = viewModel { WeiboDetailsModel(model) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "微博详情",
		onBack = { model.pop() }
	) { isBacking ->
		if (screenModel.weibo == null) EmptyBox()
		else {
			if (app.isPortrait) Portrait(
				model = screenModel,
				weibo = screenModel.weibo,
				comments = screenModel.comments
			)
			else Landscape(
				model = screenModel,
				weibo = screenModel.weibo,
				comments = screenModel.comments
			)

			LaunchOnce(screenModel.launchFlag) {
				screenModel.init(screenModel.weibo)
			}
		}
	}
}