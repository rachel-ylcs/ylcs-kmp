package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.ThemeColor
import love.yinlin.data.Data
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.extension.DateEx
import love.yinlin.extension.itemKey
import love.yinlin.platform.app
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.msg.ScreenPartMsg

@Composable
private fun UserInfoCard(
	user: WeiboUser,
	isFollowed: Boolean,
	onFollowClick: (Boolean) -> Unit,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.padding(horizontal = 10.dp, vertical = 5.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		OffsetLayout(y = (-37).dp) {
			WebImage(
				uri = user.info.avatar,
				key = DateEx.TodayString,
				contentScale = ContentScale.Crop,
				circle = true,
				modifier = Modifier.size(64.dp)
			)
		}
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(20.dp)
			) {
				Text(
					text = user.info.name,
					color = MaterialTheme.colorScheme.primary,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				ClickIcon(
					icon = if (isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
					color = MaterialTheme.colorScheme.primary,
					size = 24.dp,
					onClick = { onFollowClick(!isFollowed) }
				)
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Text(
					text = "关注 ${user.followNum}",
					modifier = Modifier.weight(1f)
				)
				Text(
					text = "粉丝 ${user.fansNum}",
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Composable
private fun UserAlbumItem(
	album: WeiboAlbum,
	onAlbumClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.clickable(onClick = onAlbumClick),
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		WebImage(
			uri = album.pic,
			modifier = Modifier.size(50.dp)
		)
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			Text(
				text = album.title,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.fillMaxWidth()
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(text = album.num)
				Text(
					text = album.time,
					color = ThemeColor.fade,
					style = MaterialTheme.typography.bodyMedium,
					textAlign = TextAlign.End,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Stable
class ScreenWeiboUser(model: AppModel, args: Args) : Screen<ScreenWeiboUser.Args>(model) {
	@Stable
	@Serializable
	data class Args(val id: String) : Screen.Args

	private val id = args.id
	private val grid = WeiboGridData()
	private var user: WeiboUser? by mutableStateOf(null)
	private var albums: List<WeiboAlbum>? by mutableStateOf(null)

	private fun onFollowClick(user: WeiboUser, isFollow: Boolean) {
		val weiboUsers = app.config.weiboUsers
		if (isFollow) {
			if (!weiboUsers.contains { it.id == user.info.id }) weiboUsers += user.info
		}
		else weiboUsers -= user.info
	}

	private fun onAlbumClick(album: WeiboAlbum) {
		navigate(ScreenWeiboAlbum.Args(album.containerId, album.title))
	}

	@Composable
	private fun Portrait(
		user: WeiboUser,
		albums: List<WeiboAlbum>?,
		grid: WeiboGridData
	) {
		LazyColumn(modifier = Modifier.fillMaxSize()) {
			item(key = "UserInfoCard".itemKey) {
				WebImage(
					uri = user.background,
					modifier = Modifier.fillMaxWidth().height(150.dp),
					contentScale = ContentScale.Crop,
					alpha = 0.8f
				)
				UserInfoCard(
					user = user,
					isFollowed = app.config.weiboUsers.contains { it.id == user.info.id },
					onFollowClick = { onFollowClick(user, it) },
					modifier = Modifier.fillMaxWidth()
				)
				HorizontalDivider(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp))
			}
			if (albums != null) {
				items(
					items = albums,
					key = { it.containerId }
				) {
					UserAlbumItem(
						album = it,
						onAlbumClick = { onAlbumClick(it) },
						modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
					)
				}
			}
			item(key = "Text".itemKey) {
				Text(
					text = "最新微博",
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.titleMedium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
				)
			}
			items(
				items = grid.items,
				key = { it.id }
			) { weibo ->
				WeiboCard(
					weibo = weibo,
					modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
				)
			}
		}
	}

	@Composable
	private fun Landscape(
		user: WeiboUser,
		albums: List<WeiboAlbum>?,
		grid: WeiboGridData
	) {
		Row(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier.width(300.dp).fillMaxHeight(),
				shadowElevation = 5.dp
			) {
				Column(modifier = Modifier.fillMaxSize()) {
					WebImage(
						uri = user.background,
						key = DateEx.TodayString,
						modifier = Modifier.fillMaxWidth().height(150.dp),
						contentScale = ContentScale.Crop,
						alpha = 0.8f
					)
					UserInfoCard(
						user = user,
						isFollowed = app.config.weiboUsers.contains { it.id == user.info.id },
						onFollowClick = { onFollowClick(user, it) },
						modifier = Modifier.fillMaxWidth()
					)
					HorizontalDivider(modifier = Modifier.padding(horizontal = 5.dp))
					Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 10.dp)) {
						if (albums == null) SimpleLoadingBox()
						else {
							if (albums.isEmpty()) SimpleEmptyBox()
							else LazyColumn(
								modifier = Modifier.fillMaxWidth(),
								contentPadding = PaddingValues(horizontal = 10.dp),
								verticalArrangement = Arrangement.spacedBy(10.dp)
							) {
								items(
									items = albums,
									key = { it.containerId }
								) {
									UserAlbumItem(
										album = it,
										onAlbumClick = { onAlbumClick(it) },
										modifier = Modifier.fillMaxWidth()
									)
								}
							}
						}
					}
				}
			}
			Space(10.dp)
			Surface(
				modifier = Modifier.weight(1f).fillMaxHeight(),
				shadowElevation = 5.dp
			) {
				StatefulBox(
					state = grid.state,
					modifier = Modifier.fillMaxSize()
				) {
					WeiboGrid(
						modifier = Modifier.fillMaxSize(),
						items = grid.items
					)
				}
			}
		}
	}

	override suspend fun initialize() {
		launch {
			val data = WeiboAPI.getWeiboUser(id)
			user = if (data is Data.Success) data.data else null
			user?.let {
				grid.requestWeibo(listOf(it.info.id))
			}
		}
		launch {
			val data = WeiboAPI.getWeiboUserAlbum(id)
			albums = if (data is Data.Success) data.data else null
		}
	}

	@Composable
	override fun content() {
		CompositionLocalProvider(LocalWeiboProcessor provides part<ScreenPartMsg>().processor) {
			SubScreen(
				modifier = Modifier.fillMaxSize(),
				title = user?.info?.name ?: "",
				onBack = { pop() },
				slot = slot
			) {
				if (user == null) LoadingBox()
				else user?.let { user ->
					if (app.isPortrait) Portrait(
						user = user,
						albums = albums,
						grid = grid
					)
					else Landscape(
						user = user,
						albums = albums,
						grid = grid
					)
				}
			}
		}
	}
}