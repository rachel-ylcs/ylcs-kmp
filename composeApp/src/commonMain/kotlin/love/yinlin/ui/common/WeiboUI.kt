package love.yinlin.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ensureActive
import love.yinlin.api.WeiboAPI
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.text.RichText
import love.yinlin.ui.component.image.WebImage
import love.yinlin.data.weibo.Weibo
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.data.Data
import love.yinlin.data.RequestError
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.platform.Coroutines
import love.yinlin.ui.screen.msg.MsgModel

class WeiboGridData {
	var state by mutableStateOf(BoxState.EMPTY)
	var items by mutableStateOf(emptyList<Weibo>())

	suspend fun requestWeibo(users: List<String>) {
		if (state != BoxState.LOADING) {
			state = BoxState.LOADING
			state = if (users.isEmpty()) BoxState.EMPTY
			else {
				val newItems = mutableMapOf<String, Weibo>()
				Coroutines.io {
					for (id in users) {
						val result = WeiboAPI.getUserWeibo(id)
						if (result is Data.Success) newItems += result.data.associateBy { it.id }
						else if (result is Data.Error && result.type == RequestError.Canceled) {
							state = BoxState.EMPTY
							ensureActive()
						}
					}
				}
				items = newItems.map { it.value }.sortedDescending()
				if (newItems.isEmpty()) BoxState.NETWORK_ERROR else BoxState.CONTENT
			}
		}
	}
}

@Composable
private fun WeiboIconValue(
	icon: ImageVector,
	text: String,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
	) {
		MiniIcon(imageVector = icon, size = 16.dp)
		Text(text = text)
	}
}

@Composable
fun WeiboDataBar(
	like: Int,
	comment: Int,
	repost: Int,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		WeiboIconValue(
			icon = Icons.Filled.ThumbUp,
			text = like.toString(),
			modifier = Modifier.weight(1f)
		)
		WeiboIconValue(
			icon = Icons.AutoMirrored.Filled.Comment,
			text = comment.toString(),
			modifier = Modifier.weight(1f)
		)
		WeiboIconValue(
			icon = Icons.Filled.Share,
			text = repost.toString(),
			modifier = Modifier.weight(1f)
		)
	}
}

@Composable
fun WeiboUserBar(
	avatar: String,
	name: String,
	time: String,
	location: String,
	modifier: Modifier = Modifier,
	onAvatarClick: () -> Unit
) {
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		WebImage(
			uri = avatar,
			key = DateEx.currentDateString,
			modifier = Modifier.size(50.dp),
			circle = true,
			onClick = onAvatarClick
		)
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			Text(
				modifier = Modifier.fillMaxWidth(),
				text = name,
				color = MaterialTheme.colorScheme.primary,
				style = MaterialTheme.typography.titleMedium
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
			) {
				Text(
					modifier = Modifier.weight(1f),
					text = time,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					overflow = TextOverflow.Ellipsis
				)
				Text(
					text = location,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}

@Composable
fun WeiboLayout(
	weibo: Weibo,
	onAvatarClick: (WeiboUserInfo) -> Unit,
	onLinkClick: (String) -> Unit,
	onTopicClick: (String) -> Unit,
	onAtClick: (String) -> Unit,
	onImageClick: (List<Picture>, Int) -> Unit,
	onVideoClick: (Picture) -> Unit
) {
	WeiboUserBar(
		modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
		avatar = weibo.info.avatar,
		name = weibo.info.name,
		time = weibo.time,
		location = weibo.location,
		onAvatarClick = { onAvatarClick(weibo.info) }
	)
	RichText(
		text = weibo.text,
		modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
		overflow = TextOverflow.Ellipsis,
		onLinkClick = onLinkClick,
		onTopicClick = onTopicClick,
		onAtClick = onAtClick
	)
	if (weibo.pictures.isNotEmpty()) {
		NineGrid(
			pics = weibo.pictures,
			modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
			onImageClick = { onImageClick(weibo.pictures, it) },
			onVideoClick = onVideoClick
		)
	}
	WeiboDataBar(
		like = weibo.likeNum,
		comment = weibo.commentNum,
		repost = weibo.repostNum,
		modifier = Modifier.fillMaxWidth()
	)
}

@Composable
fun WeiboCard(
	weibo: Weibo,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
	onAvatarClick: (WeiboUserInfo) -> Unit,
	onLinkClick: (String) -> Unit,
	onTopicClick: (String) -> Unit,
	onAtClick: (String) -> Unit,
	onImageClick: (List<Picture>, Int) -> Unit,
	onVideoClick: (Picture) -> Unit
) {
	ElevatedCard(
		modifier = modifier,
		colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
		onClick = onClick
	) {
		Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
			WeiboLayout(
				weibo = weibo,
				onAvatarClick = onAvatarClick,
				onLinkClick = onLinkClick,
				onTopicClick = onTopicClick,
				onAtClick = onAtClick,
				onImageClick = onImageClick,
				onVideoClick = onVideoClick
			)
		}
	}
}

@Composable
fun WeiboGrid(
	model: MsgModel,
	items: List<Weibo>,
	modifier: Modifier = Modifier
) {
	LazyVerticalStaggeredGrid(
		columns = StaggeredGridCells.Adaptive(300.dp),
		modifier = modifier,
		contentPadding = PaddingValues(10.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalItemSpacing = 10.dp
	) {
		items(
			items = items,
			key = { it.id }
		) { weibo ->
			WeiboCard(
				weibo = weibo,
				modifier = Modifier.fillMaxWidth(),
				onClick = { model.onWeiboClick(weibo) },
				onAvatarClick = { model.onWeiboAvatarClick(it) },
				onLinkClick = { model.onWeiboLinkClick(it) },
				onTopicClick = { model.onWeiboTopicClick(it) },
				onAtClick = { model.onWeiboAtClick(it) },
				onImageClick = { pics, current -> model.onWeiboPicClick(pics, current) },
				onVideoClick = { model.onWeiboVideoClick(it) }
			)
		}
	}
}