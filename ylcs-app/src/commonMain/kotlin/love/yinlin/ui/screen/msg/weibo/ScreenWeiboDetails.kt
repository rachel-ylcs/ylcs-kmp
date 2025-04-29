package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.Orientation
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.extension.itemKey
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.text.RichText

@Stable
class ScreenWeiboDetails(model: AppModel) : CommonSubScreen(model) {
	private val weibo: Weibo? = msgPart.currentWeibo
	private var comments: List<WeiboComment>? by mutableStateOf(null)

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
	private fun Portrait(weibo: Weibo) {
		LazyColumn(modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
			item(key = "WeiboLayout".itemKey) {
				WeiboLayout(weibo = weibo)
			}
			comments?.let { weiboComments ->
				item(key = "HorizontalDivider".itemKey) {
					HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
				}
				items(
					items = weiboComments,
					key = { it.id }
				) {
					WeiboCommentCard(comment = it)
				}
			}
		}
	}

	@Composable
	private fun Landscape(weibo: Weibo) {
		val processor = LocalWeiboProcessor.current
		Row(modifier = Modifier.fillMaxSize().padding(10.dp)) {
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
				val weiboComments = comments
				if (weiboComments == null) LoadingBox()
				else if (weiboComments.isEmpty()) EmptyBox()
				else LazyColumn(modifier = Modifier.fillMaxSize()) {
					items(
						items = weiboComments,
						key = { it.id }
					) {
						WeiboCommentCard(comment = it)
					}
				}
			}
		}
	}

	override suspend fun initialize() {
		weibo?.let {
			val data = WeiboAPI.getWeiboDetails(it.id)
			comments = if (data is Data.Success) data.data else emptyList()
		}
	}

	override val title: String = "微博详情"

	@Composable
	override fun SubContent(orientation: Orientation) {
		CompositionLocalProvider(LocalWeiboProcessor provides msgPart.processor) {
			weibo?.let {
				when (orientation) {
					Orientation.PORTRAIT -> Portrait(weibo = it)
					Orientation.LANDSCAPE -> Landscape(weibo = it)
					Orientation.SQUARE -> {}
				}
			} ?: EmptyBox()
		}
	}
}