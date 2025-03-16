package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.ScreenModel
import love.yinlin.common.screen
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.extension.Saver
import love.yinlin.platform.app
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.RichText

private class WeiboDetailsModel(model: AppModel) : ScreenModel() {
	val msgModel = model.mainModel.msgModel
	val weibo: Weibo? = msgModel.currentWeibo
	var comments: List<WeiboComment>? by mutableStateOf(null)

	override fun initialize() {
		launch {
			weibo?.let {
				val data = WeiboAPI.getWeiboDetails(it.id)
				comments = if (data is Data.Success) data.data else emptyList()
			}
		}
	}
}

@Composable
private fun WeiboCommentCard(comment: WeiboComment) {
	Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
		WeiboUserBar(
			info = comment.info,
			time = comment.timeString,
			location = comment.location,
			padding = PaddingValues(bottom = 5.dp)
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
				Column(modifier = Modifier.fillMaxWidth().padding(5.dp)) {
					for (subComment in subComments) {
						WeiboUserBar(
							info = subComment.info,
							location = subComment.location,
							time = subComment.timeString,
							padding = PaddingValues(bottom = 5.dp)
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
	weibo: Weibo,
	comments: List<WeiboComment>?
) {
	LazyColumn(
		modifier = Modifier.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface)
			.padding(start = 10.dp, end = 10.dp, top = 5.dp)
	) {
		item(key = Saver.key("WeiboLayout")) {
			WeiboLayout(weibo = weibo)
		}
		if (comments != null) {
			item(key = Saver.key("HorizontalDivider")) {
				HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
			}
			items(
				items = comments,
				key = { it.id }
			) {
				WeiboCommentCard(comment = it)
			}
		}
	}
}

@Composable
private fun Landscape(
	weibo: Weibo,
	comments: List<WeiboComment>?
) {
	val processor = LocalWeiboProcessor.current
	Row(
		modifier = Modifier.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface)
			.padding(10.dp)
	) {
		Column(modifier = Modifier.width(360.dp).fillMaxHeight()) {
			WeiboUserBar(
				info = weibo.info,
				time = weibo.timeString,
				location = weibo.location,
				padding = PaddingValues(bottom = 10.dp)
			)
			RichText(
				text = weibo.text,
				modifier = Modifier.fillMaxWidth().weight(1f),
				overflow = TextOverflow.Ellipsis,
				onLinkClick = { processor.onWeiboLinkClick(it) },
				onTopicClick = { processor.onWeiboTopicClick(it) },
				onAtClick = { processor.onWeiboAtClick(it) }
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
					modifier = Modifier.fillMaxWidth(),
					onImageClick = { processor.onWeiboPicClick(weibo.pictures, it) },
					onVideoClick = { processor.onWeiboVideoClick(it) }
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
					WeiboCommentCard(comment = it)
				}
			}
		}
	}
}

@Composable
fun ScreenWeiboDetails(model: AppModel) {
	val screenModel = screen { WeiboDetailsModel(model) }

	CompositionLocalProvider(LocalWeiboProcessor provides model.mainModel.msgModel.processor) {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "微博详情",
			onBack = { model.pop() }
		) {
			if (screenModel.weibo == null) EmptyBox()
			else {
				if (app.isPortrait) Portrait(
					weibo = screenModel.weibo,
					comments = screenModel.comments
				)
				else Landscape(
					weibo = screenModel.weibo,
					comments = screenModel.comments
				)
			}
		}
	}
}