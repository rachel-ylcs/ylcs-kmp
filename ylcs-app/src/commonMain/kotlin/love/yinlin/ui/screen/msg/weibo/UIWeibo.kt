package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.api.WeiboAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.localComposition
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.NineGrid
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.text.RichText

@Stable
interface WeiboProcessor {
	fun onWeiboClick(weibo: Weibo)
	fun onWeiboAvatarClick(info: WeiboUserInfo)
	fun onWeiboLinkClick(arg: String)
	fun onWeiboTopicClick(arg: String)
	fun onWeiboAtClick(arg: String)
	fun onWeiboPicClick(pics: List<Picture>, current: Int)
	fun onWeiboVideoClick(pic: Picture)
}

val LocalWeiboProcessor = localComposition<WeiboProcessor>()

@Stable
class WeiboGridData {
	var state by mutableStateOf(BoxState.EMPTY)
	var items by mutableStateOf(emptyList<Weibo>())

	suspend fun requestWeibo(users: List<String>) {
		if (state != BoxState.LOADING) {
			state = BoxState.LOADING
			state = if (users.isEmpty()) BoxState.EMPTY
			else {
				val newItems = mutableMapOf<String, Weibo>()
				for (id in users) {
					val result = WeiboAPI.getUserWeibo(id)
					if (result is Data.Success) newItems += result.data.associateBy { it.id }
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
		MiniIcon(icon = icon, size = 16.dp)
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
	info: WeiboUserInfo,
	time: String,
	location: String,
	padding: PaddingValues
) {
	val processor = LocalWeiboProcessor.current
	Row(
		modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(padding),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
			WebImage(
				uri = info.avatar,
				key = DateEx.TodayString,
				contentScale = ContentScale.Crop,
				circle = true,
				modifier = Modifier.matchParentSize(),
				onClick = { processor.onWeiboAvatarClick(info) }
			)
		}
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			Text(
				modifier = Modifier.fillMaxWidth(),
				text = info.name,
				color = MaterialTheme.colorScheme.primary,
				style = MaterialTheme.typography.titleMedium
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
			) {
				Text(
					text = time,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				Text(
					text = location,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}

@Composable
fun WeiboLayout(weibo: Weibo) {
	val processor = LocalWeiboProcessor.current
	WeiboUserBar(
		info = weibo.info,
		time = weibo.timeString,
		location = weibo.location,
		padding = PaddingValues(bottom = 10.dp)
	)
	RichText(
		text = weibo.text,
		modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
		overflow = TextOverflow.Ellipsis,
		onLinkClick = { processor.onWeiboLinkClick(it) },
		onTopicClick = { processor.onWeiboTopicClick(it) },
		onAtClick = { processor.onWeiboAtClick(it) }
	)
	if (weibo.pictures.isNotEmpty()) {
		NineGrid(
			pics = weibo.pictures,
			modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
			onImageClick = { processor.onWeiboPicClick(weibo.pictures, it) },
			onVideoClick = { processor.onWeiboVideoClick(it) }
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
	modifier: Modifier = Modifier
) {
	val processor = LocalWeiboProcessor.current
	Surface(
		modifier = modifier,
		shape = MaterialTheme.shapes.extraLarge,
		shadowElevation = 5.dp
	) {
		Column(modifier = Modifier.fillMaxWidth().clickable {
			processor.onWeiboClick(weibo)
		}.padding(10.dp)) {
			WeiboLayout(weibo = weibo)
		}
	}
}

@Composable
fun WeiboGrid(
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
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}