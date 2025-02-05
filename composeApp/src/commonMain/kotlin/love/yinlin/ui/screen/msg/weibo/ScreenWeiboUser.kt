package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import love.yinlin.AppModel
import love.yinlin.ThemeColor
import love.yinlin.api.WeiboAPI
import love.yinlin.app
import love.yinlin.data.Data
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.extension.DateEx
import love.yinlin.extension.LaunchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.platform.Coroutines
import love.yinlin.ui.common.WeiboCard
import love.yinlin.ui.common.WeiboGrid
import love.yinlin.ui.component.ClickIcon
import love.yinlin.ui.component.EmptyBox
import love.yinlin.ui.component.SimpleEmptyBox
import love.yinlin.ui.component.SimpleLoadingBox
import love.yinlin.ui.component.Space
import love.yinlin.ui.component.SubScreen
import love.yinlin.ui.component.WebImage
import love.yinlin.ui.common.WeiboGridData
import love.yinlin.ui.component.StatefulBox

class WeiboUserModel(model: AppModel) : ViewModel() {
	val msgModel = model.mainModel.msgModel
	val launchFlag = LaunchFlag()
	val grid = WeiboGridData()
	var user: WeiboUser? by mutableStateOf(null)
	var albums: List<WeiboAlbum>? by mutableStateOf(null)

	fun onFollowClick(user: WeiboUser) {

	}

	fun onAlbumClick(album: WeiboAlbum) {

	}
}

@Composable
private fun UserInfoCard(
	user: WeiboUser,
	onFollowClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.padding(start = 10.dp, end = 10.dp, top = 5.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		WebImage(
			uri = user.info.avatar,
			key = DateEx.currentDateString,
			modifier = Modifier.size(64.dp).offset(y = (-20).dp),
			circle = true
		)
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
					imageVector = Icons.Filled.Favorite,
					color = MaterialTheme.colorScheme.primary,
					size = 24.dp,
					onClick = onFollowClick
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
			key = DateEx.currentDateString,
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

@Composable
private fun Portrait(
	model: WeiboUserModel,
	user: WeiboUser,
	albums: List<WeiboAlbum>?,
	grid: WeiboGridData,
	onFollowClick: () -> Unit,
	onAlbumClick: (WeiboAlbum) -> Unit
) {
	LazyColumn(modifier = Modifier.fillMaxSize()) {
		item(key = -1) {
			WebImage(
				uri = user.background,
				key = DateEx.currentDateString,
				modifier = Modifier.fillMaxWidth().height(150.dp),
				contentScale = ContentScale.Crop,
				alpha = 0.8f
			)
			UserInfoCard(
				user = user,
				onFollowClick = onFollowClick,
				modifier = Modifier.fillMaxWidth()
			)
			HorizontalDivider(modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp))
		}
		if (albums != null) {
			items(
				items = albums,
				key = { it.containerId }
			) {
				UserAlbumItem(
					album = it,
					onAlbumClick = { onAlbumClick(it) },
					modifier = Modifier.fillMaxWidth().padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
				)
			}
		}
		items(
			items = grid.items,
			key = { it.id }
		) { weibo ->
			WeiboCard(
				weibo = weibo,
				modifier = Modifier.fillMaxWidth().padding(start = 5.dp, end = 5.dp, bottom = 5.dp),
				onClick = { model.msgModel.onWeiboClick(weibo) },
				onAvatarClick = { model.msgModel.onWeiboAvatarClick(it) },
				onLinkClick = { model.msgModel.onWeiboLinkClick(it) },
				onTopicClick = { model.msgModel.onWeiboTopicClick(it) },
				onAtClick = { model.msgModel.onWeiboAtClick(it) }
			)
		}
	}
}

@Composable
private fun Landscape(
	model: WeiboUserModel,
	user: WeiboUser,
	albums: List<WeiboAlbum>?,
	grid: WeiboGridData,
	onFollowClick: () -> Unit,
	onAlbumClick: (WeiboAlbum) -> Unit
) {
	Row(modifier = Modifier.fillMaxSize()) {
		Surface(
			modifier = Modifier.width(300.dp).fillMaxHeight(),
			shadowElevation = 5.dp
		) {
			Column(modifier = Modifier.fillMaxSize()) {
				WebImage(
					uri = user.background,
					key = DateEx.currentDateString,
					modifier = Modifier.fillMaxWidth().height(150.dp),
					contentScale = ContentScale.Crop,
					alpha = 0.8f
				)
				UserInfoCard(
					user = user,
					onFollowClick = onFollowClick,
					modifier = Modifier.fillMaxWidth()
				)
				HorizontalDivider(modifier = Modifier.padding(horizontal = 5.dp))
				Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 5.dp)) {
					if (albums == null) SimpleLoadingBox()
					else {
						if (albums.isEmpty()) SimpleEmptyBox()
						else LazyColumn(
							modifier = Modifier.fillMaxWidth(),
							contentPadding = PaddingValues(horizontal = 5.dp),
							verticalArrangement = Arrangement.spacedBy(5.dp)
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
					model = model.msgModel,
					modifier = Modifier.fillMaxSize(),
					items = grid.items
				)
			}
		}
	}
}

@Composable
fun ScreenWeiboUser(model: AppModel, id: String) {
	val screenModel = viewModel { WeiboUserModel(model) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = screenModel.user?.info?.name ?: "",
		onBack = { model.pop() }
	) {
		if (screenModel.user == null) EmptyBox()
		else screenModel.user?.let { user ->
			if (app.isPortrait) Portrait(
				model = screenModel,
				user = user,
				albums = screenModel.albums,
				grid = screenModel.grid,
				onFollowClick = { screenModel.onFollowClick(user) },
				onAlbumClick = { screenModel.onAlbumClick(it) }
			)
			else Landscape(
				model = screenModel,
				user = user,
				albums = screenModel.albums,
				grid = screenModel.grid,
				onFollowClick = { screenModel.onFollowClick(user) },
				onAlbumClick = { screenModel.onAlbumClick(it) }
			)
		}

		LaunchOnce(screenModel.launchFlag) {
			launch {
				screenModel.user = Coroutines.io {
					val data = WeiboAPI.getWeiboUser(id)
					if (data is Data.Success) data.data else null
				}
				screenModel.user?.let {
					screenModel.grid.requestWeibo(listOf(it.info.id))
				}
			}
			launch {
				screenModel.albums = Coroutines.io {
					val data = WeiboAPI.getWeiboUserAlbum(id)
					if (data is Data.Success) data.data else null
				}
			}
		}
	}
}