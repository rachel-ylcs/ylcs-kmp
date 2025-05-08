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
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.extension.DateEx
import love.yinlin.extension.itemKey
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.screen.SubScreen

@Composable
private fun UserInfoCard(
	user: WeiboUser,
	isFollowed: Boolean,
	onFollowClick: (Boolean) -> Unit,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.padding(ThemeValue.Padding.Value),
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
	) {
		OffsetLayout(y = -ThemeValue.Size.MediumImage / 4) {
			WebImage(
				uri = user.info.avatar,
				key = DateEx.TodayString,
				contentScale = ContentScale.Crop,
				circle = true,
				modifier = Modifier.size(ThemeValue.Size.MediumImage)
			)
		}
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = user.info.name,
					style = MaterialTheme.typography.labelLarge,
					color = MaterialTheme.colorScheme.primary,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				ClickIcon(
					icon = if (isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
					color = MaterialTheme.colorScheme.primary,
					onClick = { onFollowClick(!isFollowed) }
				)
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
			) {
				Text(
					text = "关注 ${user.followNum}",
					modifier = Modifier.weight(1f),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
				Text(
					text = "粉丝 ${user.fansNum}",
					modifier = Modifier.weight(1f),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
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
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
		verticalAlignment = Alignment.CenterVertically
	) {
		WebImage(
			uri = album.pic,
			modifier = Modifier.size(ThemeValue.Size.SmallImage)
		)
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			Text(
				text = album.title,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.fillMaxWidth()
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = album.num,
					style = MaterialTheme.typography.bodyMedium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
				Text(
					text = album.time,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					style = MaterialTheme.typography.bodySmall,
					textAlign = TextAlign.End,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Stable
class ScreenWeiboUser(model: AppModel, private val args: Args) : SubScreen<ScreenWeiboUser.Args>(model) {
	@Stable
	@Serializable
	data class Args(val id: String)

	private val grid = WeiboGridData()
	private var user: WeiboUser? by mutableStateOf(null)
	private var albums: List<WeiboAlbum>? by mutableStateOf(null)

	private fun onFollowClick(user: WeiboUser, isFollow: Boolean) {
		val weiboUsers = app.config.weiboUsers
		if (isFollow) {
			if (!weiboUsers.contains { it.id == user.info.id }) weiboUsers += user.info
		}
		else weiboUsers.removeAll { it.id == user.info.id }
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
		LazyColumn(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
			item(key = "UserInfoCard".itemKey) {
				WebImage(
					uri = user.background,
					modifier = Modifier.fillMaxWidth().aspectRatio(2f),
					contentScale = ContentScale.Crop,
					alpha = 0.8f
				)
				UserInfoCard(
					user = user,
					isFollowed = app.config.weiboUsers.contains { it.id == user.info.id },
					onFollowClick = { onFollowClick(user, it) },
					modifier = Modifier.fillMaxWidth()
				)
				HorizontalDivider(modifier = Modifier.padding(ThemeValue.Padding.EqualValue))
			}
			if (albums != null) {
				items(
					items = albums,
					key = { it.containerId }
				) {
					UserAlbumItem(
						album = it,
						onAlbumClick = { onAlbumClick(it) },
						modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue)
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
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue)
				)
			}
			items(
				items = grid.items,
				key = { it.id }
			) { weibo ->
				WeiboCard(
					weibo = weibo,
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue)
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
		Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
			Surface(
				modifier = Modifier.width(ThemeValue.Size.CardWidth).fillMaxHeight(),
				shadowElevation = ThemeValue.Shadow.Surface
			) {
				Column(modifier = Modifier.fillMaxSize()) {
					WebImage(
						uri = user.background,
						key = DateEx.TodayString,
						modifier = Modifier.fillMaxWidth().aspectRatio(2f),
						contentScale = ContentScale.Crop,
						alpha = 0.8f
					)
					UserInfoCard(
						user = user,
						isFollowed = app.config.weiboUsers.contains { it.id == user.info.id },
						onFollowClick = { onFollowClick(user, it) },
						modifier = Modifier.fillMaxWidth()
					)
					HorizontalDivider(modifier = Modifier.padding(horizontal = ThemeValue.Padding.HorizontalSpace))
					Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = ThemeValue.Padding.VerticalSpace)) {
						if (albums == null) SimpleLoadingBox()
						else {
							if (albums.isEmpty()) SimpleEmptyBox()
							else LazyColumn(
								modifier = Modifier.fillMaxWidth(),
								contentPadding = ThemeValue.Padding.EqualValue,
								verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
			Space()
			Surface(
				modifier = Modifier.weight(1f).fillMaxHeight(),
				shadowElevation = ThemeValue.Shadow.Surface
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
			val data = WeiboAPI.getWeiboUser(args.id)
			user = if (data is Data.Success) data.data else null
			user?.let {
				grid.requestWeibo(listOf(it.info.id))
			}
		}
		launch {
			val data = WeiboAPI.getWeiboUserAlbum(args.id)
			albums = if (data is Data.Success) data.data else null
		}
	}

	override val title: String by derivedStateOf { user?.info?.name ?: "" }

	@Composable
	override fun SubContent(device: Device) {
		CompositionLocalProvider(LocalWeiboProcessor provides msgPart.processor) {
			user?.let {
				when (device.type) {
					Device.Type.PORTRAIT -> Portrait(user = it, albums = albums, grid = grid)
					Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(user = it, albums = albums, grid = grid)
				}
			} ?: LoadingBox()
		}
	}
}